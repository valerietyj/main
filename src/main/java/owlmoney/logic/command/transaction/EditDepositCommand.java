package owlmoney.logic.command.transaction;

import static owlmoney.commons.log.LogsCenter.getLogger;

import java.util.logging.Logger;

import owlmoney.logic.command.Command;
import owlmoney.model.bank.exception.BankException;
import owlmoney.model.profile.Profile;
import owlmoney.model.transaction.exception.TransactionException;
import owlmoney.ui.Ui;

/**
 * Executes EditDepositCommand to edit a deposit transaction.
 */
public class EditDepositCommand extends Command {
    private final String accountName;
    private final String amount;
    private final String date;
    private final String description;
    private final int index;
    private static final Logger logger = getLogger(EditDepositCommand.class);

    /**
     * Creates an instance of EditDepositCommand.
     *
     * @param name        Bank account name.
     * @param amount      New deposit amount if any.
     * @param date        New date of deposit if any.
     * @param description New description of deposit if any.
     * @param index       Transaction number.
     */
    public EditDepositCommand(String name, String amount, String date, String description, int index) {
        this.accountName = name;
        this.amount = amount;
        this.date = date;
        this.description = description;
        this.index = index;
    }

    /**
     * Executes the function to edit a deposit transaction.
     *
     * @param profile Profile of the user.
     * @param ui      Ui of OwlMoney.
     * @return false so OwlMoney will not terminate yet.
     * @throws BankException        If bank account does not exist.
     * @throws TransactionException If incorrect date format.
     */
    public boolean execute(Profile profile, Ui ui) throws BankException, TransactionException {
        profile.profileEditDeposit(index, accountName, description, amount, date, ui);
        logger.info("Successful execution of EditDepositCommand");
        return this.isExit;
    }
}
