package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;
import ca.concordia.filesystem.datastructures.FNode;

import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileSystemManager {

    private int MAXFILES = 5;
    private int MAXBLOCKS = 10;
    private static FileSystemManager instance = null;
    private RandomAccessFile disk = null;
    private final FileChannel channel;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    private static final int BLOCK_SIZE = 128; // Example block size

    private FEntry[] inodeTable; // Array of inodes
    private FNode[] fnodes;

    public FileSystemManager(String filename, int totalSize) throws Exception {
        // totalSize = metadataSize + (MAXBLOCKS × BLOCKSIZE)

        // Prevent multiple initializations
        if (instance != null) {
            throw new IllegalStateException("FileSystemManager is already initialized.");
        }

        // Initialize the virtual disk file
        File file = new File(filename);
        this.disk = new RandomAccessFile(file, "rw");

        if (this.disk.length() < totalSize) {
            this.disk.setLength(totalSize);
        }

        this.channel = this.disk.getChannel();

        // Initialize constants
        this.MAXFILES = 5;
        this.MAXBLOCKS = 10;

        // Initialize metadata tables
        this.inodeTable = new FEntry[MAXFILES];
        this.fnodes = new FNode[MAXBLOCKS];

        // Fill with empty structures
        for (int i = 0; i < MAXFILES; i++) {
            inodeTable[i] = new FEntry("", (short) 0, (short) -1);
        }
        for (int i = 0; i < MAXBLOCKS; i++) {
            fnodes[i] = new FNode(-1);
        }

        instance = this; // Set instance

        System.out.println("FileSystemManager initialized successfully.");
    }

    // createFile Implementation
    public void createFile(String filename) throws Exception {
        rwLock.writeLock().lock();
        try {
            // Validate the filename
            if (filename == null || filename.isEmpty()) {
                throw new Exception("ERROR: filename is null or empty");
            }
            if (filename.length() > 11) {
                throw new Exception("ERROR: filename too large");
            }

            for (int i = 0; i < MAXFILES; i++) {
                FEntry entry = inodeTable[i];
                if (entry != null && filename.equals(entry.getFilename())) {
                    throw new Exception("ERROR: file " + filename + " already exists");
                }
            }

            // Find a free FEntry slot
            int freeIndex = -1;
            for (int i = 0; i < MAXFILES; i++) {
                FEntry entry = inodeTable[i];
                if (entry == null || entry.getFilename() == null || entry.getFilename().isEmpty()) {
                    freeIndex = i;
                    break;
                }
            }

            if (freeIndex == -1) {
                throw new Exception("ERROR: no free file entries available");
            }

            // Create the new entry (size = 0, firstBlock = -1)
            FEntry newFile = new FEntry(filename, (short)0, (short)-1);
            inodeTable[freeIndex] = newFile;

            // Persist metadata to disk
            persistMetadata();

            System.out.println("SUCCESS: file created -> " + filename);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void deleteFile(String filename) throws Exception {
        rwLock.writeLock().lock();
        try {
            // Validate filename
            if (filename == null || filename.isEmpty()) {
                throw new Exception("ERROR: filename is null or empty");
            }

            // Locate the file in the FEntry table
            int fileIndex = -1;
            FEntry target = null;
            for (int i = 0; i < MAXFILES; i++) {
                FEntry entry = inodeTable[i];
                if (entry != null && filename.equals(entry.getFilename())) {
                    fileIndex = i;
                    target = entry;
                    break;
                }
            }

            if (fileIndex == -1 || target == null) {
                throw new Exception("ERROR: file " + filename + " does not exist");
            }

            // Free all blocks in its FNode chain
            int fnodeIndex = target.getFirstBlock();
            while (fnodeIndex != -1 && fnodeIndex < MAXBLOCKS) {
                FNode node = fnodes[fnodeIndex];
                if (node == null) break;

                int blockIdx = node.getBlockIndex();
                if (blockIdx >= 0) {
                    long dataOffset = calculateDataOffset(blockIdx);
                    ByteBuffer zeroes = ByteBuffer.allocate(BLOCK_SIZE);
                    channel.write(zeroes, dataOffset);
                }

                // Mark node as unused
                node.setBlockIndex(-1);
                int next = node.getNext();
                node.setNext(-1);
                fnodeIndex = next;
            }

            // Clear the file entry
            inodeTable[fileIndex] = new FEntry("", (short) 0, (short) -1);

            // Persist metadata to disk
            persistMetadata();

            System.out.println("SUCCESS: file deleted -> " + filename);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    // writeFile Implementation
    public void writeFile(String filename, byte[] contents) throws Exception {
        rwLock.writeLock().lock();
        try {
            // Validate filename
            if (filename == null || filename.isEmpty()) {
                throw new Exception("ERROR: filename is null or empty");
            }

            // Find the file entry
            FEntry target = null;
            int fileIndex = -1;
            for (int i = 0; i < MAXFILES; i++) {
                FEntry entry = inodeTable[i];
                if (entry != null && filename.equals(entry.getFilename())) {
                    target = entry;
                    fileIndex = i;
                    break;
                }
            }
            if (target == null) {
                throw new Exception("ERROR: file " + filename + " does not exist");
            }

            // Calculate number of blocks needed
            int numBlocks = (int) Math.ceil((double) contents.length / BLOCK_SIZE);
            if (numBlocks > MAXBLOCKS) {
                throw new Exception("ERROR: file too large");
            }

            // Find free FNodes before modifying anything
            java.util.List<Integer> freeNodes = new java.util.ArrayList<>();
            for (int i = 0; i < MAXBLOCKS && freeNodes.size() < numBlocks; i++) {
                if (fnodes[i].getBlockIndex() < 0) { // free node
                    freeNodes.add(i);
                }
            }
            if (freeNodes.size() < numBlocks) {
                throw new Exception("ERROR: not enough free blocks available");
            }

            // Write file data to new blocks
            for (int i = 0; i < freeNodes.size(); i++) {
                int nodeIndex = freeNodes.get(i);

                int start = i * BLOCK_SIZE;
                int end = Math.min(start + BLOCK_SIZE, contents.length);
                byte[] chunk = java.util.Arrays.copyOfRange(contents, start, end);

                long dataOffset = calculateDataOffset(nodeIndex);

                ByteBuffer buf = ByteBuffer.allocate(BLOCK_SIZE);
                buf.put(chunk);
                buf.flip();
                channel.write(buf, dataOffset);

                fnodes[nodeIndex].setBlockIndex(nodeIndex);

                if (i < freeNodes.size() - 1) {
                    fnodes[nodeIndex].setNext(freeNodes.get(i + 1));
                } else {
                    fnodes[nodeIndex].setNext(-1);
                }
            }

            // Store old block chain (cleanup done later)
            int oldFirst = target.getFirstBlock();

            // Update file metadata
            target.setFilesize((short) contents.length);
            target.setFirstBlock(freeNodes.get(0).shortValue());

            // Persist metadata
            persistMetadata();

            // Free old blocks
            int oldIndex = oldFirst;
            while (oldIndex != -1 && oldIndex < MAXBLOCKS) {
                FNode node = fnodes[oldIndex];
                if (node == null) break;

                // Zero out old block data
                long offset = calculateDataOffset(oldIndex);
                ByteBuffer zero = ByteBuffer.allocate(BLOCK_SIZE);
                channel.write(zero, offset);

                int next = node.getNext();
                node.setBlockIndex(-1);
                node.setNext(-1);
                oldIndex = next;
            }

            System.out.println("SUCCESS: file written -> " + filename + " (" + contents.length + " bytes)");

        } finally {
            rwLock.writeLock().unlock();
        }
    }

    // readFile Implementation
    public byte[] readFile(String filename) throws Exception {
        rwLock.readLock().lock();
        try {
            // Validate filename
            if (filename == null || filename.isEmpty()) {
                throw new Exception("ERROR: filename is null or empty");
            }

            // Find the file entry
            FEntry target = null;
            for (FEntry entry : inodeTable) {
                if (entry != null && filename.equals(entry.getFilename())) {
                    target = entry;
                    break;
                }
            }

            if (target == null) {
                throw new Exception("ERROR: file " + filename + " does not exist");
            }

            short firstBlock = target.getFirstBlock();
            if (firstBlock < 0) {
                return new byte[0]; // Empty file
            }

            short size = target.getFilesize();
            byte[] data = new byte[size];
            int bytesRead = 0;

            // Go over FNode chain
            int currentNodeIndex = firstBlock;

            while (currentNodeIndex != -1 && bytesRead < size) {
                FNode node = fnodes[currentNodeIndex];
                if (node == null || node.getBlockIndex() < 0) {
                    throw new Exception("ERROR: corrupted fnode chain for " + filename);
                }

                long blockPos = calculateDataOffset(node.getBlockIndex());

                int bytesToRead = Math.min(BLOCK_SIZE, size - bytesRead);
                ByteBuffer buf = ByteBuffer.allocate(bytesToRead);
                channel.read(buf, blockPos);
                buf.flip();
                buf.get(data, bytesRead, bytesToRead);

                bytesRead += bytesToRead;
                currentNodeIndex = node.getNext();
            }

            System.out.println("SUCCESS: file read -> " + filename + " (" + bytesRead + " bytes)");
            return data;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    // listFiles Implementation
    public String[] listFiles() {
        rwLock.readLock().lock();
        try {
            java.util.List<String> names = new java.util.ArrayList<>(); // Create a temporary list to store filenames

            // Iterate over all file entries in the inode table
            for (FEntry entry : inodeTable) {
                // Check if this entry is valid
                if (entry != null && entry.getFilename() != null && !entry.getFilename().isEmpty()) {
                    names.add(entry.getFilename()); // Add filename to the list
                }
            }

            return names.toArray(new String[0]); // Convert the list to a String array and return it
        } finally {
            rwLock.readLock().unlock();
        }
    }

    // Saves all filesystem metadata to the start of the disk file. (FEntries + FNodes)
    private void persistMetadata() throws IOException {
        rwLock.writeLock().lock();
        try {
            long offset = 0;  // <-- NEW: track absolute write position ourselves

            // Write all FEntries
            for (int i = 0; i < MAXFILES; i++) {
                FEntry entry = inodeTable[i];
                byte[] nameBytes = new byte[11]; // fixed-size filename field

                if (entry != null && entry.getFilename() != null) {
                    byte[] src = entry.getFilename().getBytes("UTF-8");
                    int copyLen = Math.min(src.length, 11);
                    System.arraycopy(src, 0, nameBytes, 0, copyLen);
                }

                // Filename (11 bytes)
                channel.write(ByteBuffer.wrap(nameBytes), offset);
                offset += 11;    // <-- NEW instead of seek()

                // File size (2 bytes)
                short size = (entry != null) ? entry.getFilesize() : 0;
                ByteBuffer sizeBuf = ByteBuffer.allocate(2);
                sizeBuf.putShort(size);
                sizeBuf.flip();
                channel.write(sizeBuf, offset);
                offset += 2;     // <-- NEW instead of seek()

                // First block (2 bytes)
                short firstBlock = (entry != null) ? entry.getFirstBlock() : -1;
                ByteBuffer fbBuf = ByteBuffer.allocate(2);
                fbBuf.putShort(firstBlock);
                fbBuf.flip();
                channel.write(fbBuf, offset);
                offset += 2;     // <-- NEW instead of seek()
            }

            // Write all FNodes
            for (int i = 0; i < MAXBLOCKS; i++) {
                FNode node = fnodes[i];
                short blockIndex = (short) ((node != null) ? node.getBlockIndex() : -1);
                short next = (short) ((node != null) ? node.getNext() : -1);

                ByteBuffer buf = ByteBuffer.allocate(4);
                buf.putShort(blockIndex);
                buf.putShort(next);
                buf.flip();

                channel.write(buf, offset);
                offset += 4;     // <-- NEW instead of seek()
            }

            channel.force(true);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    //Calculates where a given data block starts on the disk file.
    private long calculateDataOffset(int blockIndex) {
        // Metadata section size = (15 * MAXFILES) + (4 * MAXBLOCKS)
        int metadataSize = (15 * MAXFILES) + (4 * MAXBLOCKS);
        return metadataSize + ((long) blockIndex * BLOCK_SIZE);
    }
}
