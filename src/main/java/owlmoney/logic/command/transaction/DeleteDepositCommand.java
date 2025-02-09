package owlmoney.logic.command.transaction;

import static owlmoney.commons.log.LogsCenter.getLogger;

import java.util.logging.Logger;

import owlmoney.logic.command.Command;
import owlmoney.model.bank.exception.BankException;
import owlmoney.model.profile.Profile;
import owlmoney.model.transaction.exception.TransactionException;
import owlmoney.ui.Ui;

/**
 * Executes DeleteDepositCommand to delete a deposit transaction.
 */
public class DeleteDepositCommand extends Command {
    private final int expNumber;
    private final String from;
    private static final Logger logger = getLogger(EditDepositCommand.class);

    /**
     * Creates an instance of DeleteDepositCommand.
     *
     * @param bankName Bank account name.
     * @param index    Transaction number.
     */
    public DeleteDepositCommand(String bankName, int index) {
        this.expNumber = index;
        this.from = bankName;
    }

    /**
     * Executes the function to delete a deposit transaction.
     *
     * @param profile Profile of the user.
     * @param ui      Ui of OwlMoney.
     * @return false so OwlMoney will not terminate yet.
     * @throws BankException        If bank account does not exist.
     * @throws TransactionException If transaction is not a deposit.
     */
    public boolean execute(Profile profile, Ui ui) throws BankException, TransactionException {
        profile.profileDeleteDeposit(this.expNumber, this.from, ui, false);
        logger.info("Successful execution of DeleteDepositCommand");
        return this.isExit;
    }
}
