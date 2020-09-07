import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ArrayList;



// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;

    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    class Node {
        Block block;
        int height;
        UTXOPool pool;

        Node(Block block, int height, UTXOPool pool) {
            this.block = block;
            this.height = height;
            this.pool = new UTXOPool(pool);
        }
    }

    private TransactionPool transactionPool;
    private HashMap<byte[], Node> chain;
    private Node maxHeightNode;

    public BlockChain(Block genesisBlock) {
        UTXOPool utxoPool = new UTXOPool();
        addCoinbaseTxIntoUTXOPool(genesisBlock, utxoPool);
        Node genesisNode = new Node(genesisBlock,0,utxoPool);
        this.maxHeightNode = genesisNode;
        this.transactionPool = new TransactionPool();
        this.chain = new HashMap<byte[],Node>();
        chain.put(genesisBlock.getHash(), genesisNode);
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        return maxHeightNode.block;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        return maxHeightNode.pool;
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return transactionPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        if(block.getPrevBlockHash()==null) return false;

        if(!chain.containsKey(block.getPrevBlockHash())) return false;

        Node prevNode = chain.get(block.getPrevBlockHash());

        if (prevNode.height < maxHeightNode.height - CUT_OFF_AGE) return false;

        TxHandler txHandler = new TxHandler(prevNode.pool);

        Transaction[] validTxs = txHandler.handleTxs(block.getTransactions().toArray(new Transaction[0]));

        // All transactions are valid
        if (validTxs.length < block.getTransactions().size()) return false;


        UTXOPool utxoPool = txHandler.getUTXOPool();
        addCoinbaseTxIntoUTXOPool(block, utxoPool);
        Node newNode = new Node(block,prevNode.height+1,utxoPool);
        chain.put(block.getHash(), newNode);
        if(newNode.height>maxHeightNode.height) maxHeightNode = newNode;
        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        transactionPool.addTransaction(tx);
    }

    private void addCoinbaseTxIntoUTXOPool(Block block, UTXOPool pool) {
        Transaction tx = block.getCoinbase();
        for (int i = 0; i < tx.numOutputs(); i++) {
            Transaction.Output out = tx.getOutput(i);
            UTXO utxo = new UTXO(tx.getHash(), i);
            pool.addUTXO(utxo, out);
        }
    }
}
