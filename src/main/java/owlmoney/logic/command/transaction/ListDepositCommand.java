package owlmoney.logic.command.transaction;

import static owlmoney.commons.log.LogsCenter.getLogger;

import java.util.logging.Logger;

import owlmoney.logic.command.Command;
import owlmoney.model.bank.exception.BankException;
import owlmoney.model.profile.Profile;
import owlmoney.model.transaction.exception.TransactionException;
import owlmoney.ui.Ui;

/**
 * Executes ListDepositCommand to list deposits.
 */
public class ListDepositCommand extends Command {
    private final String accountName;
    private final int displayNum;
    private static final Logger logger = getLogger(ListDepositCommand.class);

    /**
     * Creates an instance of ListDepositCommand.
     *
     * @param name       Bank account name.
     * @param displayNum Number of deposits to display.
     */
    public ListDepositCommand(String name, int displayNum) {
        this.accountName = name;
        this.displayNum = displayNum;
    }

    /**
     * Executes the function to list the specified number of deposit transactions.
     *
     * @param profile Profile of the user.
     * @param ui      Ui of OwlMoney.
     * @return false so OwlMoney will not terminate yet.
     * @throws BankException        If bank account does not exist.
     * @throws TransactionException If invalid transaction
     */
    public boolean execute(Profile profile, Ui ui) throws BankException, TransactionException {
        profile.profileListDeposit(accountName, ui, displayNum);
        logger.info("Successful execution of ListDepositCommand");
        return this.isExit;
    }
}
