package owlmoney.logic.command.transaction;

import static owlmoney.commons.log.LogsCenter.getLogger;

import java.util.Date;
import java.util.logging.Logger;

import owlmoney.logic.command.Command;
import owlmoney.model.bank.exception.BankException;
import owlmoney.model.profile.Profile;
import owlmoney.model.transaction.Deposit;
import owlmoney.model.transaction.Transaction;
import owlmoney.ui.Ui;

/**
 * Executes AddDepositCommand to add a new deposit transaction.
 */
public class AddDepositCommand extends Command {

    private final String accountName;
    private final double amount;
    private final Date date;
    private final String description;
    private static final String BANK_TYPE = "bank";
    private static final String TRANSACTION_CATEGORY_DEPOSIT = "deposit";
    private static final Logger logger = getLogger(AddDepositCommand.class);

    /**
     * Creates an instance of AddDepositCommand.
     *
     * @param name        Bank account name.
     * @param amount      Amount deposited.
     * @param date        Date of deposit.
     * @param description Description of deposit.
     */
    public AddDepositCommand(String name, double amount, Date date, String description) {
        this.accountName = name;
        this.amount = amount;
        this.date = date;
        this.description = description;
    }

    /**
     * Executes the function to add a new deposit to the bank.
     *
     * @param profile Profile of the user.
     * @param ui      Ui of OwlMoney.
     * @return false so OwlMoney will not terminate yet.
     * @throws BankException If bank account does not exist.
     */
    public boolean execute(Profile profile, Ui ui) throws BankException {
        Transaction newDeposit = new Deposit(this.description, this.amount, this.date,
                TRANSACTION_CATEGORY_DEPOSIT);
        profile.profileAddNewDeposit(accountName, newDeposit, ui, BANK_TYPE);
        logger.info("Successful execution of AddDepositCommand");
        return this.isExit;
    }
}
