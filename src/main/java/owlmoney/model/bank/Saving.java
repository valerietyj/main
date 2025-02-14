package owlmoney.model.bank;

import static owlmoney.commons.log.LogsCenter.getLogger;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

import owlmoney.model.bank.exception.BankException;
import owlmoney.model.transaction.Deposit;
import owlmoney.model.transaction.Expenditure;
import owlmoney.model.transaction.RecurringExpenditureList;
import owlmoney.model.transaction.Transaction;
import owlmoney.model.transaction.TransactionList;
import owlmoney.model.transaction.exception.TransactionException;
import owlmoney.storage.Storage;
import owlmoney.ui.Ui;

/**
 * Contains the details in a savings account in addition to a normal bank account.
 */
public class Saving extends Bank {

    private double income;
    private static final String SAVING = "saving";
    private static final String ACCOUNT_TYPE = "bank";
    private Date nextIncomeDate;
    private RecurringExpenditureList recurringExpenditures;
    private static final String SAVING_TRANSACTION_LIST_FILE_NAME = "_saving_transactionList.csv";
    private static final String SAVING_RECURRING_TRANSACTION_LIST_FILE_NAME = "_saving_recurring_transactionList.csv";
    private Storage storage;
    private static final String FILE_PATH = "data/";
    private static final String INCOME_CATEGORY = "Income";
    private static final int OBJ_DOES_NOT_EXIST = -1;
    private static final Logger logger = getLogger(Saving.class);

    /**
     * Creates an instance of a savings account.
     *
     * @param name          The name of the bank account.
     * @param currentAmount The current amount of money in  the bank account.
     * @param income        The amount of money that is credited monthly into the account.
     */
    public Saving(String name, double currentAmount, double income) {
        super(name, currentAmount);
        this.income = income;
        this.type = SAVING;
        this.transactions = new TransactionList();
        this.recurringExpenditures = new RecurringExpenditureList();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DATE, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.add(Calendar.MONTH, 1);
        nextIncomeDate = calendar.getTime();
        this.storage = new Storage(FILE_PATH);
    }

    /**
     * Creates an instance of a savings account from persistent storage storage.
     *
     * @param name          The name of the bank account.
     * @param currentAmount The current amount of money in  the bank account.
     * @param income        The amount of money that is credited monthly into the account.
     */
    public Saving(String name, double currentAmount, double income, Date nextIncomeDate) {
        super(name, currentAmount);
        this.income = income;
        this.type = SAVING;
        this.transactions = new TransactionList();
        this.recurringExpenditures = new RecurringExpenditureList();
        this.nextIncomeDate = nextIncomeDate;
        this.storage = new Storage(FILE_PATH);
    }

    /**
     * Updates the next income date and current amount if an income has been earned.
     *
     * @return If there is an update to the income.
     * @throws BankException If unable to add income.
     */
    private boolean earnedIncome(Ui ui) throws BankException {
        if (new Date().compareTo(nextIncomeDate) >= 0) {
            if (this.getCurrentAmount() + this.income > MAX_AMOUNT) {
                throw new BankException("Amount in bank account cannot exceed 9 digits");
            }
            if (income > 0) {
                Deposit addIncome = new Deposit("Income", this.income, this.nextIncomeDate, INCOME_CATEGORY);
                addDepositTransaction(addIncome, ui, ACCOUNT_TYPE);
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(nextIncomeDate);
            calendar.add(Calendar.MONTH, 1);
            nextIncomeDate = calendar.getTime();
            logger.info("Successful added earned income");
            return true;
        }
        return false;
    }

    /**
     * Gets the income of the bank accounts.
     *
     * @return the income of the bank account which includes income and type.
     */
    @Override
    public double getIncome() {
        return income;
    }

    /**
     * Adds an expenditure tied to this instance of the bank account.
     *
     * @param expenditure an instance of expenditure.
     * @param ui          required for printing.
     * @param bankType    Type of bank to add expenditure into.
     * @throws BankException If bank account becomes negative after adding expenditure.
     */
    @Override
    public void addInExpenditure(Transaction expenditure, Ui ui, String bankType) throws BankException {
        if (!"bank".equals(bankType) && !"savings transfer".equals(bankType)) {
            logger.warning("Bonds cannot be added to this account");
            throw new BankException("Bonds cannot be added to this account");
        }
        if (expenditure.getAmount() > this.getCurrentAmount()) {
            logger.warning("Bank account cannot have a negative amount");
            throw new BankException("Bank account cannot have a negative amount");
        } else {
            transactions.addExpenditureToList(expenditure, ui, bankType);
            deductFromAmount(expenditure.getAmount());
            logger.info("Successful adding of expenditure");
        }
    }

    /**
     * Lists the deposits in the current bank account.
     *
     * @param ui                Ui of OwlMoney.
     * @param depositsToDisplay Number of deposits to list.
     * @throws TransactionException If no deposit is found.
     */
    @Override
    void listAllDeposit(Ui ui, int depositsToDisplay) throws TransactionException {
        transactions.listDeposit(ui, depositsToDisplay);
        logger.info("Successfully listed deposits");
    }

    /**
     * Lists the expenditures in the current bank account.
     *
     * @param ui                    Ui of OwlMoney.
     * @param expendituresToDisplay Number of expenditure to list.
     * @throws TransactionException If no expenditure is found.
     */
    @Override
    void listAllExpenditure(Ui ui, int expendituresToDisplay) throws TransactionException {
        transactions.listExpenditure(ui, expendituresToDisplay);
        logger.info("Successfully listed expenditures");
    }

    /**
     * Deletes an expenditure tied to this bank account.
     *
     * @param expenditureIndex The index of the expenditure in ExpenditureList.
     * @param ui               required for printing.
     * @param isCreditCardBill Is the command affecting a credit card bill.
     * @throws TransactionException If invalid transaction.
     * @throws BankException If the bank amount exceeds 9 digits.
     */
    @Override
    public void deleteExpenditure(int expenditureIndex, Ui ui, boolean isCreditCardBill)
            throws TransactionException, BankException {
        double expenditureAmount = transactions.getExpenditureAmount(expenditureIndex, isCreditCardBill);
        if (this.getCurrentAmount() + expenditureAmount > MAX_AMOUNT) {
            logger.warning("The amount in the bank account cannot exceed 9 digits");
            throw new BankException("The amount in the bank account cannot exceed 9 digits");
        }
        addToAmount(transactions.deleteExpenditureFromList(expenditureIndex, ui, isCreditCardBill));
        logger.info("Successfully deleted expenditure");
    }

    /**
     * Sets a new income of the current bank account.
     *
     * @param newIncome Income to set.
     */
    @Override
    void setIncome(double newIncome) {
        this.income = newIncome;
    }

    /**
     * Edits the expenditure details from the current bank account.
     *
     * @param expenditureIndex Transaction number.
     * @param description      New description.
     * @param amount           New amount.
     * @param date             New date.
     * @param category         New category.
     * @param ui               Ui of OwlMoney.
     * @throws TransactionException If incorrect date format.
     * @throws BankException        If amount is negative after editing expenditure.
     */
    @Override
    void editExpenditureDetails(
            int expenditureIndex, String description, String amount, String date, String category, Ui ui)
            throws TransactionException, BankException {
        if (!(amount == null || amount.isBlank()) && (this.getCurrentAmount()
                + transactions.getExpenditureAmount(expenditureIndex, false) - Double.parseDouble(amount)
                > MAX_AMOUNT)) {
            logger.warning("The amount in the bank cannot exceed 9 digits");
            throw new BankException("The amount in the bank cannot exceed 9 digits");
        }
        if (!(amount == null || amount.isBlank()) && this.getCurrentAmount()
                + transactions.getExpenditureAmount(expenditureIndex, false) < Double.parseDouble(amount)) {
            logger.warning("Bank account cannot have a negative amount");
            throw new BankException("Bank account cannot have a negative amount");
        }
        double oldAmount = transactions.getExpenditureAmount(expenditureIndex, false);
        double newAmount = transactions.editExpenditure(expenditureIndex, description, amount, date, category, ui);
        this.addToAmount(oldAmount);
        this.deductFromAmount(newAmount);
        logger.info("Successfully edited expenditure");
    }

    /**
     * Edits the deposit details from the current bank account.
     *
     * @param depositIndex Transaction number.
     * @param description  New description.
     * @param amount       New amount.
     * @param date         New date.
     * @param ui           Ui of OwlMoney.
     * @throws TransactionException If incorrect date format.
     * @throws BankException        If amount becomes negative after editing deposit.
     */
    @Override
    void editDepositDetails(int depositIndex, String description, String amount, String date, Ui ui)
            throws TransactionException, BankException {
        if (!(amount == null || amount.isBlank()) && (this.getCurrentAmount()
                - transactions.getDepositValue(depositIndex, false) + Double.parseDouble(amount)
                > MAX_AMOUNT)) {
            logger.warning("The amount in the bank cannot exceed 9 digits");
            throw new BankException("The amount in the bank cannot exceed 9 digits");
        } else if (!(amount == null || amount.isBlank()) && this.getCurrentAmount()
                + Double.parseDouble(amount) < transactions.getDepositValue(depositIndex, false)) {
            logger.warning("Bank account cannot have a negative amount");
            throw new BankException("Bank account cannot have a negative amount");
        }
        double oldAmount = transactions.getDepositValue(depositIndex, false);
        double newAmount = transactions.editDeposit(depositIndex, description, amount, date, ui);
        this.addToAmount(newAmount);
        this.deductFromAmount(oldAmount);
        logger.info("Successfully edited deposit");
    }

    /**
     * Adds a new deposit to the current bank account.
     *
     * @param deposit  Deposit to add.
     * @param ui       Ui of OwlMoney.
     * @param bankType Type of bank to add deposit into.
     */
    @Override
    void addDepositTransaction(Transaction deposit, Ui ui, String bankType) throws BankException {
        if (!"bank".equals(bankType) && !"savings transfer".equals(bankType)) {
            logger.warning("This account does not support investment account deposits");
            throw new BankException("This account does not support investment account deposits");
        }
        if (this.getCurrentAmount() + deposit.getAmount() > MAX_AMOUNT) {
            logger.warning("The amount in the bank cannot exceed 9 digits");
            throw new BankException("The amount in the bank cannot exceed 9 digits");
        }
        transactions.addDepositToList(deposit, ui, bankType);
        addToAmount(deposit.getAmount());
        logger.info("Successfully added deposit");
    }

    /**
     * Deletes a deposit from the current bank account.
     *
     * @param index Transaction number.
     * @param ui    Ui of OwlMoney.
     * @param isCardBill Is affecting credit card bill deposit.
     * @throws TransactionException If transaction is not a deposit.
     * @throws BankException        If amount becomes negative after editing deposit.
     */
    @Override
    void deleteDepositTransaction(int index, Ui ui, boolean isCardBill) throws TransactionException, BankException {
        double depositValue = transactions.getDepositValue(index, isCardBill);
        if (this.getCurrentAmount() < depositValue) {
            logger.warning("Bank account cannot have a negative amount");
            throw new BankException("Bank account cannot have a negative amount");
        } else {
            this.deductFromAmount(transactions.deleteDepositFromList(index, ui));
            logger.info("Successfully deleted deposit");
        }
    }

    /**
     * Updates the recurring expenditure to the net date and add an expenditure to expenditure list if overdue.
     *
     * @param recurringExpenditure The recurring expenditure to check.
     * @param ui                   Used for printing.
     * @return Outdated state of the expenditure.
     * @throws BankException If bank amount becomes negative.
     */
    private boolean savingUpdateRecurringExpenditure(Transaction recurringExpenditure, Ui ui)
            throws BankException {
        DateFormat dateOutputFormat = new SimpleDateFormat("dd MMMM yyyy");
        Date expenditureDate = null;
        try {
            expenditureDate = dateOutputFormat.parse(recurringExpenditure.getDate());
        } catch (ParseException e) {
            //check is already done when adding expenditure
        }
        boolean currentState = false;
        if (new Date().compareTo(expenditureDate) >= 0) {
            Transaction newExpenditure = new Expenditure(
                    recurringExpenditure.getDescription(), recurringExpenditure.getAmount(),
                    expenditureDate, recurringExpenditure.getCategory());
            addInExpenditure(newExpenditure, ui, ACCOUNT_TYPE);
            Calendar calendar = Calendar.getInstance();
            calendar.clear();
            calendar.setTime(expenditureDate);
            calendar.add(Calendar.MONTH, 1);
            recurringExpenditure.setDate(calendar.getTime());
            currentState = true;
            logger.info("Successfully added recurring expenditure transaction");
        }
        return currentState;
    }

    /**
     * Updates all recurring expenditures in the bank.
     *
     * @param ui Used for printing.
     * @throws BankException If unable to add income.
     */
    @Override
    void updateRecurringTransactions(Ui ui) throws BankException {
        boolean outdatedExpenditure;
        while (true) {
            if (!earnedIncome(ui)) {
                break;
            }
        }
        for (int i = 0; i < recurringExpenditures.getListSize();) {
            outdatedExpenditure = false;
            try {
                outdatedExpenditure = savingUpdateRecurringExpenditure(
                        recurringExpenditures.getRecurringExpenditure(i), ui);
            } catch (BankException errorMessage) {
                logger.warning("There is not enough money in the bank for: "
                        + recurringExpenditures.getRecurringExpenditure(i).getDescription());
                ui.printError("There is not enough money in the bank for: "
                        + recurringExpenditures.getRecurringExpenditure(i).getDescription());
            }
            if (!outdatedExpenditure) {
                i++;
            }
        }
    }

    /**
     * Adds a new recurring expenditure to the bank.
     *
     * @param newExpenditure New recurring expenditure to be added.
     * @param ui             Used for printing.
     * @throws TransactionException If the recurring expenditure list is full.
     */
    void savingAddRecurringExpenditure(Transaction newExpenditure, Ui ui) throws TransactionException {
        recurringExpenditures.addRecurringExpenditure(newExpenditure, ui);
        logger.info("Successfully added recurring expenditure entry");
    }

    /**
     * Deletes a recurring expenditure from the bank.
     *
     * @param index Index of the recurring expenditure.
     * @param ui    Used for printing.
     * @throws TransactionException If there are 0 recurring expenditures or index is out of range.
     */
    void savingDeleteRecurringExpenditure(int index, Ui ui) throws TransactionException {
        recurringExpenditures.deleteRecurringExpenditure(index, ui);
        logger.info("Successfully deleted recurring expenditure entry");
    }

    /**
     * Edits a recurring transaction from the bank.
     *
     * @param index       Index of the recurring expenditure.
     * @param description New description of the recurring expenditure.
     * @param amount      New amount of the recurring expenditure.
     * @param category    New category of the recurring expenditure.
     * @param ui          Used for printing.
     * @throws TransactionException If there are 0 recurring expenditures or index is out of range.
     */
    void savingEditRecurringExpenditure(int index, String description, String amount, String category, Ui ui)
            throws TransactionException {
        recurringExpenditures.editRecurringExpenditure(index, description, amount, category, ui);
        logger.info("Successfully edited recurring expenditure entry");
    }

    /**
     * Lists all recurring expenditures in the bank.
     *
     * @param ui Used for printing.
     * @throws TransactionException If there are 0 recurring expenditures.
     */
    void savingListRecurringExpenditure(Ui ui) throws TransactionException {
        recurringExpenditures.listRecurringExpenditure(ui);
        logger.info("Successfully listed recurring expenditures");
    }

    /**
     * Exports the transaction list.
     *
     * @param prependFileName the index of the bankAccount in the bankList.
     * @throws IOException if there are errors exporting the file.
     */
    @Override
    public void exportBankTransactionList(String prependFileName) throws IOException {
        ArrayList<String[]> inputData = prepareExportTransactionList();
        try {
            storage.writeFile(inputData, prependFileName + SAVING_TRANSACTION_LIST_FILE_NAME);
            logger.warning("Successfully exported: " + prependFileName + SAVING_TRANSACTION_LIST_FILE_NAME);
        } catch (IOException exceptionMessage) {
            logger.warning("Error exporting: " + prependFileName + SAVING_TRANSACTION_LIST_FILE_NAME);
            throw new IOException(exceptionMessage);
        }
    }

    /**
     * Prepares the recurring transaction list for exporting.
     *
     * @return properly formatted recurring transaction list in Arraylist that contains array of strings.
     */
    @Override
    ArrayList<String[]> prepareExportRecurringTransactionList() {
        ArrayList<String[]> exportArrayList = new ArrayList<>();
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        decimalFormat.setRoundingMode(RoundingMode.DOWN);
        SimpleDateFormat exportDateFormat = new SimpleDateFormat("dd/MM/yyyy");
        exportArrayList.add(new String[] {"description", "amount", "date", "category", "spent"});

        for (int i = 0; i < recurringExpenditures.getListSize(); i++) {
            String description = recurringExpenditures.get(i).getDescription();
            double amount = recurringExpenditures.get(i).getAmount();
            String date = exportDateFormat.format(recurringExpenditures.get(i).getDateInDateFormat());
            String category = recurringExpenditures.get(i).getCategory();
            boolean spent = recurringExpenditures.get(i).getSpent();
            String stringAmount = decimalFormat.format(amount);
            String stringSpent = String.valueOf(spent);
            exportArrayList.add(new String[] {description, stringAmount, date, category, stringSpent});
        }
        logger.info("Successfully prepared recurringExpenditureList for exporting");
        return exportArrayList;
    }

    /**
     * Exports the recurring transaction list.
     *
     * @param prependFileName the index of the bankAccount in the bankList.
     * @throws IOException   if there are errors exporting the file.
     */
    @Override
    void exportBankRecurringTransactionList(String prependFileName) throws IOException {
        ArrayList<String[]> inputData = prepareExportRecurringTransactionList();
        try {
            storage.writeFile(inputData, prependFileName
                    + SAVING_RECURRING_TRANSACTION_LIST_FILE_NAME);
            logger.info("Successfully exported: "
                    + prependFileName + SAVING_RECURRING_TRANSACTION_LIST_FILE_NAME);
        } catch (IOException e) {
            logger.warning("Error exporting: " + prependFileName + SAVING_RECURRING_TRANSACTION_LIST_FILE_NAME);
            throw new IOException(e);
        }
    }

    /**
     * Imports new expenditures one at a time.
     *
     * @param expenditure an instance of the expenditure, contained in 1 line in the saved file.
     * @param bankType    the type of bank account.
     * @throws BankException if the bank account does not support this feature.
     */
    @Override
    public void importNewExpenditure(Transaction expenditure, String bankType) throws BankException {
        if (!"bank".equals(bankType) && !"savings transfer".equals(bankType)) {
            logger.warning("Bonds cannot be added to this account");
            throw new BankException("Bonds cannot be added to this account");
        }
        if (expenditure.getAmount() > this.getCurrentAmount()) {
            logger.warning("Bank account cannot have a negative amount");
            throw new BankException("Bank account cannot have a negative amount");
        } else {
            transactions.importExpenditureToList(expenditure);
            logger.info("Successfully imported expenditure");
        }
    }

    /**
     * Imports new deposits one at a time.
     *
     * @param deposit  an instance of the deposit, contained in 1 line in the saved file.
     * @param bankType the type of deposit and bank type.
     * @throws BankException if the bank account does not support this feature.
     */
    @Override
    public void importNewDeposit(Transaction deposit, String bankType) throws BankException {
        if (!"bank".equals(bankType) && !"savings transfer".equals(bankType)) {
            logger.warning("This account does not support investment account deposits");
            throw new BankException("This account does not support investment account deposits");
        }
        transactions.importDepositToList(deposit);
        logger.info("Successfully imported deposit");
    }

    /**
     * Imports new recurring expenditures one at a time.
     *
     * @param expenditure an instance of the recurring expenditure, contained in 1 line in the saved file.
     */
    @Override
    public void importNewRecurringExpenditure(Transaction expenditure) {
        recurringExpenditures.importRecurringExpenditureToList(expenditure);
        logger.info("Successfully imported recurring expenditure");
    }

    /**
     * Gets the next income date of the bank account.
     *
     * @return the nextIncomeDate of the savings account.
     */
    @Override
    Date getNextIncomeDate() {
        return this.nextIncomeDate;
    }

    /**
     * Gets if transaction list contains the specified expenditure and deposit.
     *
     * @param cardId    The bill card id to look for in transaction list.
     * @param billDate  The bill card date to look for in transaction list.
     * @return True if transaction list contains the specified expenditure and deposit.
     */
    @Override
    public boolean isTransactionCardBillExist(UUID cardId, YearMonth billDate) {
        boolean isExpenditureFound = false;
        boolean isDepositFound = false;
        for (int i = 0; i < transactions.getSize(); i++) {
            UUID transactionCardId = transactions.get(i).getTransactionCardID();
            YearMonth transactionCardBillDate = transactions.get(i).getTransactionCardBillDate();
            boolean isExpenditure = transactions.get(i).getSpent();
            if (transactionCardId.equals(cardId) && transactionCardBillDate.equals(billDate)
                    && isExpenditure) {
                isExpenditureFound = true;
            }
            if (transactionCardId.equals(cardId) && transactionCardBillDate.equals(billDate)
                    && !isExpenditure) {
                isDepositFound = true;
            }
        }
        return isExpenditureFound && isDepositFound;
    }

    /**
     * Gets the index of the expenditure object that is a card bill expenditure
     * with specified card id and bill date.
     *
     * @param cardId    The bill card id to look for in transaction list.
     * @param billDate  The bill card date to look for in transaction list.
     * @return Index of the expenditure object if found. If not found, return -1.
     */
    @Override
    public int getCardBillExpenditureId(UUID cardId, YearMonth billDate) {
        for (int i = 0; i < transactions.getSize(); i++) {
            UUID transactionCardId = transactions.get(i).getTransactionCardID();
            YearMonth transactionCardBillDate = transactions.get(i).getTransactionCardBillDate();
            boolean isExpenditure = transactions.get(i).getSpent();
            if (transactionCardId == null) {
                continue;
            }
            if (transactionCardId.equals(cardId) && transactionCardBillDate.equals(billDate)
                    && isExpenditure) {
                return i;
            }
        }
        logger.info("Card bill expenditure does not exist");
        return OBJ_DOES_NOT_EXIST;
    }

    /**
     * Gets the index of the deposit object that is a card bill expenditure
     * with specified card id and bill date.
     *
     * @param cardId    The bill card id to look for in transaction list.
     * @param billDate  The bill card date to look for in transaction list.
     * @return Index of the deposit object if found. If not found, return -1.
     */
    @Override
    public int getCardBillDepositId(UUID cardId, YearMonth billDate) {
        for (int i = 0; i < transactions.getSize(); i++) {
            UUID transactionCardId = transactions.get(i).getTransactionCardID();
            YearMonth transactionCardBillDate = transactions.get(i).getTransactionCardBillDate();
            boolean isExpenditure = transactions.get(i).getSpent();
            if (transactionCardId == null) {
                continue;
            }
            if (transactionCardId.equals(cardId) && transactionCardBillDate.equals(billDate)
                    && !isExpenditure) {
                return i;
            }
        }
        logger.info("Card bill rebate deposit does not exist");
        return OBJ_DOES_NOT_EXIST;
    }

    /**
     * Finds the recurring expenditure that matches with the keywords specified by the user
     * for savings account.
     *
     * @param description The description keyword to match against.
     * @param category    The category keyword to match against.
     * @param ui          The object required for printing.
     * @throws TransactionException If recurring expenditure list is empty.
     */
    @Override
    public void findRecurringExpenditure(String description, String category, Ui ui) throws TransactionException {
        recurringExpenditures.findMatchingRecurringExpenditure(description, category, ui);
        logger.info("Completed finding recurring expenditures");
    }
}
