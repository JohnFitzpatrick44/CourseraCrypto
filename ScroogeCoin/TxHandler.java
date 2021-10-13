import java.util.*;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
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
        Set<UTXO> usedUtxos = new HashSet<>();
        double incomingValue = 0;
        double outgoingValue = 0;

        for(int i = 0; i < tx.getInputs().size(); i++){
            Transaction.Input transactionInput = tx.getInput(i);
            UTXO utxo = new UTXO(transactionInput.prevTxHash, transactionInput.outputIndex);

            // 1
            if(!utxoPool.contains(utxo)){
                return false;
            }

            // 2
            Transaction.Output previousTransactionOutput = utxoPool.getTxOutput(utxo);
            if(!Crypto.verifySignature(previousTransactionOutput.address, tx.getRawDataToSign(i), transactionInput.signature)){
                return false;
            }

            // 3
            if(usedUtxos.contains(utxo)){
                return false;
            }
            usedUtxos.add(utxo);

            incomingValue += previousTransactionOutput.value;
        }
        
        for(Transaction.Output transactionOutput : tx.getOutputs()){
            
            // 4
            if(transactionOutput.value < 0){
                return false;
            }

            outgoingValue += transactionOutput.value;
        }
        
        // 5
        return incomingValue >= outgoingValue;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
    List<Transaction> accepted = new ArrayList<>();
        
        for(Transaction t : possibleTxs){
            if(isValidTx(t)){
                accepted.add(t);
                updatePool(t);
            }
        }
        
        Transaction[] res = new Transaction[accepted.size()];
        
        for(int i = 0; i < accepted.size(); i++){
            res[i] = accepted.get(i);
        }
        
        return res;
    }
    
    private void updatePool(Transaction tx){
        for(Transaction.Input ti : tx.getInputs()){
            UTXO utxo = new UTXO(ti.prevTxHash, ti.outputIndex);
            utxoPool.removeUTXO(utxo);
        }

        for(int i = 0; i < tx.getOutputs().size(); i++){
            Transaction.Output out = tx.getOutput(i);
            UTXO utxo = new UTXO(tx.getHash(), i);
            utxoPool.addUTXO(utxo, out);
        }
    }
}
