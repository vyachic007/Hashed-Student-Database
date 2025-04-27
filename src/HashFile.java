import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class HashFile implements Closeable {
    private final FileChannel channel;
    private final RandomAccessFile file;
    private Block0 block0;
    private int nextBlockNumber = 1;
    private int totalBlocks = 0;
    private int cachedBlockNumber = -1;
    private Block cachedBlock = null;


    public HashFile(String filename) throws IOException, ClassNotFoundException {
        this.file = new RandomAccessFile(filename, "rw");
        this.channel = file.getChannel();

        if (file.length() == 0) {
            this.block0 = new Block0();
            writeBlock0();
            file.setLength(Block0.SIZE);
        } else {
            readBlock0();
            totalBlocks = (int) ((file.length() - Block0.SIZE) / Block.SIZE);
            nextBlockNumber = totalBlocks + 1;
        }
    }

    private Bucket findBucket(int id_zachet) {
        int bucketIndex = id_zachet % 4;
        return block0.getCatalog()[bucketIndex];
    }


    private void writeBlock0() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); //выв в память
        ObjectOutputStream oos = new ObjectOutputStream(bos);  //для серил
        oos.writeObject(block0);
        channel.write(ByteBuffer.wrap(bos.toByteArray()), 0); // зап сери д-х в файл
        channel.force(true);
    }


    private void readBlock0() throws IOException, ClassNotFoundException {
        ByteBuffer buffer = ByteBuffer.allocate(Block0.SIZE);
        channel.read(buffer, 0);
        buffer.flip();

        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(buffer.array(), 0, buffer.limit()));
        block0 = (Block0) ois.readObject();
    }


    private void writeBlock(int blockNumber, Block block) throws IOException {
        /// Обновляем кэш
        cachedBlockNumber = blockNumber;
        cachedBlock = block;

        ByteArrayOutputStream bos = new ByteArrayOutputStream(); //времен буф для серилиз
        ObjectOutputStream oos = new ObjectOutputStream(bos); //объект для серил об-в в поток
        oos.writeObject(block);
        byte[] data = bos.toByteArray();

        long position = Block0.SIZE + (blockNumber - 1L) * Block.SIZE;
        channel.write(ByteBuffer.wrap(data), position);
        channel.force(true);
    }



    private Block readBlock(int blockNumber) throws IOException, ClassNotFoundException {
        if (cachedBlockNumber == blockNumber && cachedBlock != null) {
            return cachedBlock;
        }
        long position = Block0.SIZE + (blockNumber - 1L) * Block.SIZE;
        if (position >= file.length()) return null;

        ByteBuffer buffer = ByteBuffer.allocate(Block.SIZE);
        channel.read(buffer, position);
        buffer.flip();

        ObjectInputStream ois = new ObjectInputStream( // чит объекты
                new ByteArrayInputStream(buffer.array(), 0, buffer.limit())); //чит массив байт из буфера
        Block block = (Block) ois.readObject();

        cachedBlockNumber = blockNumber;
        cachedBlock = block;
        return block;
    }



    public void addStudent(Zap student) throws IOException, ClassNotFoundException {
        Bucket bucket = findBucket(student.getId_zachet());

        if (bucket.getNf() == -1) {
            int newBlockNumber = allocateBlock();
            Block newBlock = new Block();
            newBlock.addZap(student);

            bucket.setNf(newBlockNumber);
            bucket.setNl(newBlockNumber);
            writeBlock(newBlockNumber, newBlock);
            writeBlock0();
            return;
        }
        int currentBlockNumber = bucket.getNf();
        while (currentBlockNumber != -1) {
            Block currentBlock = readBlock(currentBlockNumber);
            if (!currentBlock.isFull()) {
                currentBlock.addZap(student);
                writeBlock(currentBlockNumber, currentBlock);
                return;
            }
            currentBlockNumber = currentBlock.getNextb();
        }
        int newBlockNumber = allocateBlock();
        Block newBlock = new Block();
        newBlock.addZap(student);

        int lastInBucket = bucket.getNl();
        Block lastBlock = readBlock(lastInBucket);
        lastBlock.setNextb(newBlockNumber);
        bucket.setNl(newBlockNumber);

        writeBlock(lastInBucket, lastBlock);
        writeBlock(newBlockNumber, newBlock);
        writeBlock0();
    }


    public Zap findStudent(int id_zachet) throws IOException, ClassNotFoundException {
        Bucket bucket = findBucket(id_zachet);
        int currentBlockNumber = bucket.getNf();

        while (currentBlockNumber != -1) {
            Block block = readBlock(currentBlockNumber);
            if (block == null) return null;

            int index = block.findZapIndexById(id_zachet);
            if (index != -1) {
                return block.zap_block[index];
            }
            currentBlockNumber = block.getNextb();
        }
        return null;
    }



    public boolean deleteStudent(int id_zachet) throws IOException, ClassNotFoundException {
        Bucket bucket = findBucket(id_zachet);

        int currentBlockNumber = bucket.getNf();
        int lastBlockNumber = -1;
        Block lastBlock = null;

        int targetBlockNumber = -1;
        int targetIndex = -1;
        Block targetBlock = null;

        while (currentBlockNumber != -1) {
            Block currentBlock = readBlock(currentBlockNumber);
            if (currentBlock == null) return false;

            if (currentBlock.getNextb() == -1) {
                lastBlock = currentBlock;
                lastBlockNumber = currentBlockNumber;
            }

            int idx = currentBlock.findZapIndexById(id_zachet);

            if (idx != -1) {
                targetBlock = currentBlock;
                targetBlockNumber = currentBlockNumber;
                targetIndex = idx;
            }
            currentBlockNumber = currentBlock.getNextb();
        }
        if (targetBlock == null) {
            return false;
        }
        if (targetBlockNumber == lastBlockNumber && targetIndex == lastBlock.size() - 1) {
            targetBlock.updateOrRemoveZapById(id_zachet, null);
            writeBlock(targetBlockNumber, targetBlock);

            if (targetBlock.isEmpty()) {
                removeBlockFromBucket(bucket, targetBlockNumber, findPreviousBlockInBucket(bucket, targetBlockNumber));
                compactFileAfterDeletion(targetBlockNumber);
            }
            writeBlock0();
            return true;
        }
        Zap lastZap = lastBlock.getZap(lastBlock.size() - 1);
        targetBlock.updateOrRemoveZapById(id_zachet, lastZap);
        lastBlock.updateOrRemoveZapById(lastZap.getId_zachet(), null);

        writeBlock(targetBlockNumber, targetBlock);
        writeBlock(lastBlockNumber, lastBlock);

        if (lastBlock.isEmpty()) {
            removeBlockFromBucket(bucket, lastBlockNumber, findPreviousBlockInBucket(bucket, lastBlockNumber));
            compactFileAfterDeletion(lastBlockNumber);
        }
        writeBlock0();
        return true;
    }



    public void updateStudent(int oldId, Zap updated) throws IOException, ClassNotFoundException {
        if (deleteStudent(oldId)) {
            addStudent(updated);
        }
    }



    private int allocateBlock() throws IOException {
        int newBlockNumber = nextBlockNumber++;
        totalBlocks++;
        long position = Block0.SIZE + (newBlockNumber - 1L) * Block.SIZE;
        file.setLength(position + Block.SIZE);
        return newBlockNumber;
    }


    private void compactFileAfterDeletion(int emptyBlockNumber) throws IOException, ClassNotFoundException {
        totalBlocks--;
        nextBlockNumber--;

        if (emptyBlockNumber != totalBlocks + 1) {
            Block lastBlock = readBlock(totalBlocks + 1);
            writeBlock(emptyBlockNumber, lastBlock);
            updateBlockReferences(totalBlocks + 1, emptyBlockNumber);
        }

        file.setLength(Block0.SIZE + (long) totalBlocks * Block.SIZE);
        /// Сбрасываем кэш после удаления и изменения файла
        cachedBlockNumber = -1;
        cachedBlock = null;
    }



    private void updateBlockReferences(int oldBlockNumber, int newBlockNumber) throws IOException, ClassNotFoundException {
        for (int i = 0; i < 4; i++) {
            Bucket bucket = findBucketByIndex(i);

            int currentBlockNumber = bucket.getNf();

            while (currentBlockNumber != -1) {
                Block currentBlock = readBlock(currentBlockNumber);

                if (currentBlock != null) {
                    if (currentBlock.getNextb() == oldBlockNumber) {
                        currentBlock.setNextb(newBlockNumber);
                        writeBlock(currentBlockNumber, currentBlock);
                    }
                }
                if (bucket.getNf() == oldBlockNumber) {
                    bucket.setNf(newBlockNumber);
                }
                if (bucket.getNl() == oldBlockNumber) {
                    bucket.setNl(newBlockNumber);
                }
                currentBlockNumber = currentBlock != null ? currentBlock.getNextb() : -1;
            }
        }
    }

    private Bucket findBucketByIndex(int index) {
        return block0.getCatalog()[index];
    }



    private int findPreviousBlockInBucket(Bucket bucket, int targetBlockNumber) throws IOException, ClassNotFoundException {
        int current = bucket.getNf();
        int prev = -1;

        while (current != -1) {
            Block block = readBlock(current);
            if (current == targetBlockNumber) {
                return prev;
            }
            prev = current;
            current = block.getNextb();
        }

        return -1;
    }


    private void removeBlockFromBucket(Bucket bucket, int blockToRemove, int prevBlockNumber)
            throws IOException, ClassNotFoundException {

        Block blockToRemoveObj = readBlock(blockToRemove);
        if (blockToRemoveObj == null) return;

        if (prevBlockNumber == -1) {
            bucket.setNf(blockToRemoveObj.getNextb());

            if (bucket.getNl() == blockToRemove) {
                bucket.setNl(-1);
            }
        } else {
            Block prevBlock = readBlock(prevBlockNumber);
            if (prevBlock == null) return;

            prevBlock.setNextb(blockToRemoveObj.getNextb());
            writeBlock(prevBlockNumber, prevBlock);

            if (bucket.getNl() == blockToRemove) {
                bucket.setNl(prevBlockNumber);
            }
        }
    }



    @Override
    public void close() throws IOException {
        try {
            writeBlock0();
        } finally {
            channel.force(true);
            channel.close();
            file.close();
        }
    }
}

