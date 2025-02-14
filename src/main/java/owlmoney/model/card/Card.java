package owlmoney.model.card;

import static owlmoney.commons.log.LogsCenter.getLogger;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

import owlmoney.model.card.exception.CardException;
import owlmoney.model.transaction.Transaction;
import owlmoney.model.transaction.TransactionList;
import owlmoney.model.transaction.exception.TransactionException;
import owlmoney.storage.Storage;
import owlmoney.ui.Ui;

/**
 * Card class for initialisation of credit card object.
 */
public class Card {
    private String name;
    private double limit;
    private double rebate;
    private TransactionList paid;
    private TransactionList unpaid;
    private UUID id;
    private static final int OBJ_DOES_NOT_EXIST = -1;
    private static final int ONE_ARRAY_INDEX = 1;
    private static final int DIVIDE_BY_2 = 2;
    private Storage storage;
    private static final String FILE_PATH = "data/";
    private static final String CARD_PAID_TRANSACTION_LIST_FILE_NAME = "_card_paid_transactionList.csv";
    private static final String CARD_UNPAID_TRANSACTION_LIST_FILE_NAME = "_card_unpaid_transactionList.csv";
    private static final Logger logger = getLogger(Card.class);



    /**
     * Creates a Card with details of name, limit and rebate.
     *
     * @param name   A name for the credit card.
     * @param limit  Credit card monthly spending limit.
     * @param rebate Credit card monthly cash back rebate.
     */
    public Card(String name, double limit, double rebate) {
        this.name = name;
        this.limit = limit;
        this.rebate = rebate;
        this.paid = new TransactionList();
        this.unpaid = new TransactionList();
        this.id = UUID.randomUUID();
        this.storage = new Storage(FILE_PATH);
    }

    /**
     * Creates a Card with details of name, limit and rebate from persistent storage.
     *
     * @param name   A name for the credit card.
     * @param limit  Credit card monthly spending limit.
     * @param rebate Credit card monthly cash back rebate.
     * @param uuid The unique id of the card object.
     */
    public Card(String name, double limit, double rebate, UUID uuid) {
        this.name = name;
        this.limit = limit;
        this.rebate = rebate;
        this.paid = new TransactionList();
        this.unpaid = new TransactionList();
        this.id = uuid;
        this.storage = new Storage(FILE_PATH);
    }

    /**
     * Gets the card id of the credit card.
     *
     * @return id of the credit card.
     */
    UUID getId() {
        return id;
    }

    /**
     * Gets the card name of the credit card.
     *
     * @return name of the credit card.
     */
    String getName() {
        return this.name;
    }

    /**
     * Sets the card name for the credit card.
     *
     * @param name A name for the credit card.
     */
    void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the card limit of the credit card.
     *
     * @return card limit of the credit card.
     */
    double getLimit() {
        return this.limit;
    }


    /**
     * Sets the card limit for the credit card.
     *
     * @param limit A name for the credit card.
     */
    void setLimit(double limit) {
        this.limit = limit;
    }

    /**
     * Gets the rebate of the credit card.
     *
     * @return rebate of the credit card.
     */
    double getRebate() {
        return this.rebate;
    }

    /**
     * Sets the rebate for the credit card.
     *
     * @param rebate Rebate for the credit card.
     */
    void setRebate(double rebate) {
        this.rebate = rebate;
    }

    /**
     * Checks if expenditure exceeds remaining card limit.
     *
     * @param expenditure Expenditure to be added.
     * @throws CardException If expenditure exceeds remaining card limit.
     */
    private void checkExpExceedRemainingLimit(Transaction expenditure) throws CardException {
        LocalDate date = expenditure.getLocalDate();
        double monthAmountSpent = unpaid.getMonthAmountSpent(date.getMonthValue(), date.getYear());
        double remainingMonthAmount = limit - monthAmountSpent;
        if (expenditure.getAmount() > remainingMonthAmount) {
            logger.warning("Expenditure to be added cannot exceed remaining limit of $"
                    + remainingMonthAmount);
            throw new CardException("Expenditure to be added cannot exceed remaining limit of $"
                    + remainingMonthAmount);
        }
    }

    /**
     * Adds expenditure to the credit card unpaid transaction list.
     *
     * @param expenditure  Expenditure to be added.
     * @param ui   Ui of OwlMoney.
     * @param type Type of account to add expenditure into
     * @throws CardException If expenditure exceeds card limit.
     */
    void addInExpenditure(Transaction expenditure, Ui ui, String type) throws CardException {
        this.checkExpExceedRemainingLimit(expenditure);
        unpaid.addExpenditureToList(expenditure, ui, type);
        logger.info("Successfully added expenditure in unpaid list in " + getName());
    }

    /**
     * Adds expenditure to the credit card paid transaction list. Used in JUnit only.
     *
     * @param expenditure  Expenditure to be added.
     * @param ui   Ui of OwlMoney.
     * @param type Type of account to add expenditure into
     * @throws CardException If expenditure exceeds card limit.
     */
    void addInPaidExpenditure(Transaction expenditure, Ui ui, String type) throws CardException {
        this.checkExpExceedRemainingLimit(expenditure);
        paid.addExpenditureToList(expenditure, ui, type);
    }

    /**
     * Lists all the paid and unpaid expenditures in the current credit card.
     *
     * @param ui         Ui of OwlMoney.
     * @param displayNum Number of expenditure to list.
     */
    void listAllExpenditure(Ui ui, int displayNum) {
        int displayNumHalf = displayNum / DIVIDE_BY_2;
        try {
            ui.printMessage("Paid Expenditures:");
            paid.listExpenditure(ui, displayNumHalf);
        } catch (TransactionException e) {
            ui.printMessage("There are no paid expenditures in this card.");
        }
        try {
            ui.printMessage("");
            ui.printMessage("Unpaid Expenditures:");
            unpaid.listExpenditure(ui, displayNumHalf);
        } catch (TransactionException e) {
            ui.printMessage("There are no unpaid expenditures in this card.");
        }
        logger.info("Successfully listed paid and unpaid expenditure list in " + getName());
    }

    /**
     * Deletes an expenditure in the current credit card.
     *
     * @param exId Transaction number of the transaction.
     * @param ui   Ui of OwlMoney.
     * @throws TransactionException If invalid transaction.
     */
    void deleteExpenditure(int exId, Ui ui) throws TransactionException {
        unpaid.deleteExpenditureFromList(exId, ui, false);
        logger.info("Successfully deleted expenditure in " + getName());
    }

    /**
     * Edits the expenditure details from the current card account.
     *
     * @param expNum   Transaction number.
     * @param desc     New description.
     * @param amount   New amount.
     * @param date     New date.
     * @param category New category.
     * @param ui       Ui of OwlMoney.
     * @throws TransactionException If incorrect date format.
     * @throws CardException        If amount is negative after editing expenditure.
     */
    void editExpenditureDetails(int expNum, String desc, String amount, String date, String category, Ui ui)
            throws TransactionException, CardException {
        double remainingLimit = 0;
        if (date == null || date.isBlank()) {
            int expMonth = unpaid.getTransactionMonthByIndex(expNum);
            int expYear = unpaid.getTransactionYearByIndex(expNum);
            remainingLimit = limit - unpaid.getMonthAmountSpent(expMonth, expYear);
        } else {
            DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            Date simpleDate = null;
            try {
                simpleDate = dateFormat.parse(date);
            } catch (ParseException e) {
                logger.warning("Date parsing failed.");
                return;
            }
            LocalDate expDate = simpleDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            int expMonth = expDate.getMonthValue();
            int expYear = expDate.getYear();
            remainingLimit = limit - unpaid.getMonthAmountSpent(expMonth, expYear);
        }

        double existingExpAmount = unpaid.getExpenditureAmount(expNum, false);
        double limitLeftExcludeExistingExp = remainingLimit + existingExpAmount;
        if (!(amount == null || amount.isBlank())
                && limitLeftExcludeExistingExp < Double.parseDouble(amount)) {
            logger.warning("Edited expenditure cannot exceed $" + limitLeftExcludeExistingExp);
            throw new CardException("Edited expenditure cannot exceed $" + limitLeftExcludeExistingExp);
        }
        unpaid.editExpenditure(expNum, desc, amount, date, category, ui);
        logger.info("Successfully edited expenditure in " + getName());
    }

    /** Returns remaining limit of this current month.
     *
     * @return      Remaining limit of this current month.
     */
    public double getRemainingLimitNow() {
        LocalDate currentDate = LocalDate.now();
        int month = currentDate.getMonthValue();
        int year = currentDate.getYear();
        return limit - unpaid.getMonthAmountSpent(month, year);
    }

    /**
     * Returns true if unpaid expenditure list is empty.
     *
     * @return True if unpaid expenditure list is empty.
     */
    public boolean isEmpty() {
        return unpaid.expenditureListIsEmpty();
    }

    /**
     * Finds the transactions from the card object that matches with the keywords specified by the user.
     *
     * @param fromDate The date to search from.
     * @param toDate The date to search until.
     * @param description The description keyword to match against.
     * @param category The category keyword to match against.
     * @param ui The object required for printing.
     * @throws TransactionException If incorrect date format.
     */
    void findTransaction(String fromDate, String toDate, String description, String category, Ui ui)
            throws TransactionException {
        try {
            logger.info("Searching through: unpaid expenditure");
            ui.printMessage("Searching through: unpaid expenditure");
            unpaid.findMatchingTransaction(fromDate, toDate, description, category, ui);
        } catch (TransactionException errorMessage) {
            logger.warning(errorMessage.toString());
            ui.printError(errorMessage.getMessage());
        }
        try {
            logger.info("Searching through: paid expenditure");
            ui.printMessage("Searching through: paid expenditure");
            paid.findMatchingTransaction(fromDate, toDate, description, category, ui);
        } catch (TransactionException errorMessage) {
            logger.warning(errorMessage.toString());
            ui.printError(errorMessage.getMessage());
        }
    }

    /**
     * Returns the total amount of all unpaid card expenditures of specified date.
     *
     * @param date  The YearMonth date of unpaid card expenditures to get the total amount.
     * @return      The total amount of all unpaid card expenditures of specified date.
     */
    public double getUnpaidBillAmount(YearMonth date) {
        return unpaid.getMonthAmountSpent(date.getMonthValue(), date.getYear());
    }

    /**
     * Returns the total amount of all paid card expenditures of specified date.
     *
     * @param date  The YearMonth date of paid card expenditures to get the total amount.
     * @return      The total amount of all paid card expenditures of specified date.
     */
    public double getPaidBillAmount(YearMonth date) {
        return paid.getMonthAmountSpent(date.getMonthValue(), date.getYear());
    }

    /**
     * Transfers expenditures from unpaid list to paid list.
     *
     * @param cardDate      The YearMonth date of expenditures to transfer.
     * @param type          Type of expenditure (card or bank).
     * @throws TransactionException If invalid transaction when deleting.
     */
    void transferExpUnpaidToPaid(YearMonth cardDate, String type) throws TransactionException {
        for (int i = 0; i < unpaid.getSize(); i++) {
            int id = unpaid.getExpenditureIdByYearMonth(cardDate);
            if (id != OBJ_DOES_NOT_EXIST) {
                Transaction expenditure = unpaid.getExpenditureObjectByYearMonth(id);
                paid.addExpenditureToList(expenditure, type);
                unpaid.deleteExpenditureFromList(id + ONE_ARRAY_INDEX);
                i -= ONE_ARRAY_INDEX;
            }
        }
        logger.info("Transfer unpaid to paid function completed");
    }

    /**
     * Transfers expenditures from paid list to unpaid list.
     *
     * @param cardDate      The YearMonth date of expenditures to transfer.
     * @param type          Type of expenditure (card or bank).
     * @throws TransactionException If invalid transaction when deleting.
     */
    void transferExpPaidToUnpaid(YearMonth cardDate, String type) throws TransactionException {
        for (int i = 0; i < paid.getSize(); i++) {
            int id = paid.getExpenditureIdByYearMonth(cardDate);
            if (id != OBJ_DOES_NOT_EXIST) {
                Transaction expenditure = paid.getExpenditureObjectByYearMonth(id);
                unpaid.addExpenditureToList(expenditure, type);
                paid.deleteExpenditureFromList(id + ONE_ARRAY_INDEX);
                i -= ONE_ARRAY_INDEX;
            }
        }
        logger.info("Transfer paid to unpaid function completed");
    }

    /**
     * Prepares the paid transaction list for exporting.
     *
     * @return properly formatted paid transaction list in Arraylist that contains array of strings.
     */
    private ArrayList<String[]> prepareExportPaidTransactionList() {
        ArrayList<String[]> exportArrayList = new ArrayList<>();
        DecimalFormat decimalFormat = new DecimalFormat(".00");
        decimalFormat.setRoundingMode(RoundingMode.DOWN);
        SimpleDateFormat exportDateFormat = new SimpleDateFormat("dd/MM/yyyy");
        exportArrayList.add(new String[]{"description","amount","date","category","cardId","billDate",});
        for (int i = 0; i < paid.getSize(); i++) {
            String description = paid.get(i).getDescription();
            double amount = paid.get(i).getAmount();
            Date date = paid.get(i).getDateInDateFormat();
            String category = paid.get(i).getCategory();
            UUID cardId = paid.get(i).getTransactionCardID();
            YearMonth billDate = paid.get(i).getTransactionCardBillDate();
            String stringAmount = decimalFormat.format(amount);
            String stringDate = exportDateFormat.format(date);
            String stringUuid = "";
            if (cardId != null) {
                stringUuid = cardId.toString();
            }
            String stringBillDate = "";
            if (billDate != null) {
                stringBillDate = billDate.toString();
            }
            exportArrayList.add(new String[]
                {description, stringAmount, stringDate, category, stringUuid, stringBillDate});
        }
        logger.info("Successfully prepared " + getName()
                + " credit card paid transaction list for exporting");
        return exportArrayList;
    }

    /**
     * Exports the paid transaction list.
     *
     * @param prependFileName the index of the card in the cardList.
     * @throws IOException if there are errors exporting the file.
     */
    void exportCardPaidTransactionList(String prependFileName) throws IOException {
        ArrayList<String[]> inputData = prepareExportPaidTransactionList();
        try {
            storage.writeFile(inputData,prependFileName + CARD_PAID_TRANSACTION_LIST_FILE_NAME);
            logger.info("Successfully exported " + getName() + " credit card paid list to "
                    + CARD_UNPAID_TRANSACTION_LIST_FILE_NAME);
        } catch (IOException exceptionMessage) {
            logger.warning(exceptionMessage.toString());
            throw new IOException(exceptionMessage);
        }
    }

    /**
     * Prepares the unpaid transaction list for exporting.
     *
     * @return properly formatted unpaid transaction list in Arraylist that contains array of strings.
     */
    private ArrayList<String[]> prepareExportUnpaidTransactionList() {
        ArrayList<String[]> exportArrayList = new ArrayList<>();
        DecimalFormat decimalFormat = new DecimalFormat(".00");
        decimalFormat.setRoundingMode(RoundingMode.DOWN);
        SimpleDateFormat exportDateFormat = new SimpleDateFormat("dd/MM/yyyy");
        exportArrayList.add(new String[]{"description","amount","date","category","cardId","billDate",});
        for (int i = 0; i < unpaid.getSize(); i++) {
            String description = unpaid.get(i).getDescription();
            double amount = unpaid.get(i).getAmount();
            Date date = unpaid.get(i).getDateInDateFormat();
            String category = unpaid.get(i).getCategory();
            UUID cardId = unpaid.get(i).getTransactionCardID();
            YearMonth billDate = unpaid.get(i).getTransactionCardBillDate();
            String stringAmount = decimalFormat.format(amount);
            String stringDate = exportDateFormat.format(date);
            String stringUuid = "";
            if (cardId != null) {
                stringUuid = cardId.toString();
            }
            String stringBillDate = "";
            if (billDate != null) {
                stringBillDate = billDate.toString();
            }
            exportArrayList.add(new String[]
                {description, stringAmount, stringDate, category, stringUuid, stringBillDate});
        }
        logger.info("Successfully prepared " + getName()
                + " credit card unpaid transaction list for exporting");
        return exportArrayList;
    }

    /**
     * Exports the unpaid transaction list.
     *
     * @param prependFileName the index of the card in the cardList.
     * @throws IOException if there are errors exporting the file.
     */
    void exportCardUnpaidTransactionList(String prependFileName) throws IOException {
        ArrayList<String[]> inputData = prepareExportUnpaidTransactionList();
        try {
            storage.writeFile(inputData,prependFileName + CARD_UNPAID_TRANSACTION_LIST_FILE_NAME);
            logger.info("Successfully exported " + getName() + " credit card unpaid list to "
                + CARD_UNPAID_TRANSACTION_LIST_FILE_NAME);
        } catch (IOException exceptionMessage) {
            logger.warning(exceptionMessage.toString());
            throw new IOException(exceptionMessage);
        }
    }

    /**
     * Imports paid expenditures one at a time.
     *
     * @param newExpenditure an instance of the expenditure, contained in 1 line in the save file.
     */
    void importNewPaidExpenditure(Transaction newExpenditure) {
        paid.importExpenditureToList(newExpenditure);
    }

    /**
     * Imports unpaid expenditures one at a time.
     *
     * @param newExpenditure an instance of the expenditure, contained in 1 line in the save file.
     */
    void importNewUnpaidExpenditure(Transaction newExpenditure) {
        unpaid.importExpenditureToList(newExpenditure);
    }
}
