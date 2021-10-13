// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory
// as it would cause a memory overflow.

import java.util.ArrayList;
import java.util.HashMap;

public class BlockChain {
    private HashMap<ByteArrayWrapper, BlockNode> blockChain;
    private BlockNode maxHeightNode;
    private TransactionPool transactionPool;

    public static final int CUT_OFF_AGE = 10;


    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        this.blockChain = new HashMap<>();
        this.transactionPool = new TransactionPool();

        // Create genesis node and add coinbase to UTXO pool
        UTXOPool utxoPool = new UTXOPool();
        addCoinbaseToUTXOPool(genesisBlock, utxoPool);
        BlockNode genesisNode = new BlockNode(genesisBlock, null, utxoPool);
        this.blockChain.put(wrap(genesisBlock.getHash()), genesisNode);
        this.maxHeightNode = genesisNode;
    }

    /**
     * Get the maximum height block
     */
    public Block getMaxHeightBlock() {
        return this.maxHeightNode.block;
    }

    /**
     * Get the UTXOPool for mining a new block on top of max height block
     */
    public UTXOPool getMaxHeightUTXOPool() {
        return this.maxHeightNode.getUtxoPoolCopy();
    }

    /**
     * Get the transaction pool to mine a new block
     */
    public TransactionPool getTransactionPool() {
        return this.transactionPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * <p>
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     *
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        byte[] prevBlockHash = block.getPrevBlockHash();
        if (prevBlockHash == null){
            return false;
        }

        BlockNode parentBlockNode = blockChain.get(wrap(prevBlockHash));
        if (parentBlockNode == null) {
            return false;
        }

        TxHandler handler = new TxHandler(parentBlockNode.getUtxoPoolCopy());
        Transaction[] txs = block.getTransactions().toArray(new Transaction[0]);
        Transaction[] validTxs = handler.handleTxs(txs);

        if (validTxs.length != txs.length) {
            return false;
        }

        int proposedHeight = parentBlockNode.height + 1;
        if (proposedHeight <= this.maxHeightNode.height - CUT_OFF_AGE) {
            return false;
        }

        UTXOPool utxoPool = handler.getUTXOPool();
        addCoinbaseToUTXOPool(block, utxoPool);
        BlockNode node = new BlockNode(block, parentBlockNode, utxoPool);
        blockChain.put(wrap(block.getHash()), node);

        if (proposedHeight > this.maxHeightNode.height) {
            this.maxHeightNode = node;
        }

        return true;
    }

    /**
     * Add a transaction to the transaction pool
     */
    public void addTransaction(Transaction tx) {
        this.transactionPool.addTransaction(tx);
    }

    private void addCoinbaseToUTXOPool(Block block, UTXOPool utxoPool) {
        Transaction coinbase = block.getCoinbase();

        for (int i = 0; i < coinbase.numOutputs(); i++) {
            Transaction.Output output = coinbase.getOutput(i);
            UTXO utxo = new UTXO(coinbase.getHash(), i);
            utxoPool.addUTXO(utxo, output);
        }
    }

    private static ByteArrayWrapper wrap(byte[] arr) {
        return new ByteArrayWrapper(arr);
    }

    private class BlockNode {
        public Block block;
        public BlockNode parent;
        public ArrayList<BlockNode> children;
        public int height;

        // UTXO pool for making a new block on top of this block
        private UTXOPool utxoPool;

        public BlockNode(Block block, BlockNode parent, UTXOPool utxoPool) {
            this.block = block;
            this.parent = parent;
            this.utxoPool = utxoPool;

            this.children = new ArrayList<>();
            if (parent != null) {
                this.height = parent.height + 1;
                parent.children.add(this);
            } else {
                this.height = 1;
            }
        }

        public UTXOPool getUtxoPoolCopy() {
            return new UTXOPool(utxoPool);
        }
    }
}