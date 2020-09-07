import java.util.ArrayList;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    UTXOPool utxoPool;
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool = utxoPool;
    }

    public UTXOPool getUTXOPool()
    {
        return utxoPool;
    }
    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */

    public boolean isValidTx(Transaction tx) {
        UTXOPool uniqueUTXO = new UTXOPool();
        double inputSum = 0;
        double outputSum = 0;

        for(int i = 0;i<tx.numInputs(); i++)
        {
            Transaction.Input in = tx.getInput(i);
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);

            if(!utxoPool.contains(utxo)) return false;

            if(uniqueUTXO.contains(utxo)) return false;

            Transaction.Output output = utxoPool.getTxOutput(utxo);

            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), in.signature)) return false;

            inputSum+=output.value;

            uniqueUTXO.addUTXO(utxo, output);

        }

        // iterate over all transactions output
        for (Transaction.Output out : tx.getOutputs()) {
            // check if the output value are non-negative
            if (out.value < 0)
                return false;

            // add current output value
            outputSum += out.value;
        }

        // check if the sum of the input value is less than sum of output value
        return outputSum <= inputSum;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> ans = new ArrayList<Transaction>();
        for(Transaction tx : possibleTxs)
        {
            if(isValidTx(tx))
            {
                ans.add(tx);
                for (Transaction.Input in : tx.getInputs()) {
                    UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
                    utxoPool.removeUTXO(utxo);
                }
                for (int i = 0; i < tx.numOutputs(); i++) {
                    Transaction.Output out = tx.getOutput(i);
                    UTXO utxo = new UTXO(tx.getHash(), i);
                    utxoPool.addUTXO(utxo, out);
                }
            }
        }

        Transaction[] validTxArray = new Transaction[ans.size()];
        return ans.toArray(validTxArray);
    }

}