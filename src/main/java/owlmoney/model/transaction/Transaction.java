package owlmoney.model.transaction;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

/**
 * Contains the details of a transaction.
 */
public abstract class Transaction {

    private String description;
    private double amount;
    private Date date;
    private String category;
    private boolean spent;
    private UUID cardID;
    private YearMonth billDate;

    /**
     * Creates an instance of a transaction object.
     *
     * @param description The description that describes this expenditure.
     * @param amount      The amount of money spent in this instance of expenditure.
     * @param date        The date when this expenditure was made.
     * @param category    The category that this expenditure can be tagged to.
     */
    public Transaction(String description, double amount, Date date, String category) {
        this.description = description;
        this.amount = amount;
        this.date = date;
        this.category = category;
    }

    /**
     * Creates an overloaded instance of a transaction object for savings card bill transactions.
     *
     * @param description The description that describes this expenditure.
     * @param amount      The amount of money spent in this instance of expenditure.
     * @param date        The date when this expenditure was made.
     * @param cardId      The credit card ID.
     * @param billDate    The YearMonth date of card bill where this transaction is meant for.
     */
    public Transaction(String description, double amount, Date date, UUID cardId, YearMonth billDate) {
        this.description = description;
        this.amount = amount;
        this.date = date;
        this.category = "Credit Card";
        this.cardID = cardId;
        this.billDate = billDate;
    }

    /**
     * Sets the spent flag for this transaction.
     *
     * @param spent The flag of whether the transaction is deducting from bank amount.
     */
    void setSpent(boolean spent) {
        this.spent = spent;
    }

    /**
     * Gets the spent flag for this transaction.
     *
     * @return True if transaction deducts from bank amount.
     */
    public boolean getSpent() {
        return this.spent;
    }

    /**
     * Gets the amount of money spent in this transaction.
     *
     * @return The amount of money spent in this transaction.
     */
    public double getAmount() {
        return this.amount;
    }

    /**
     * Gets the description of the instance of expenditure.
     *
     * @return The description of the expenditure.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Gets the date that this expenditure was made in String format.
     *
     * @return The date that the expenditure was made in String format.
     */
    public String getDate() {
        DateFormat temp = new SimpleDateFormat("dd MMMM yyyy");
        return temp.format(this.date);
    }

    /**
     * Gets the date that this expenditure was made in Date format.
     *
     * @return The date that the expenditure was made in Date format.
     */
    public Date getDateInDateFormat() {
        return this.date;
    }

    /**
     * Gets the date that this expenditure was made in LocalDate format.
     *
     * @return The date that the expenditure was made in LocalDate format.
     */
    public LocalDate getLocalDate() {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return localDate;
    }

    /**
     * Gets the date that this expenditure was made in YearMonth format.
     *
     * @return The date that the expenditure was made in YearMonth format.
     */
    public YearMonth getYearMonthDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
        YearMonth date = YearMonth.parse(this.getDate(), formatter);
        return date;
    }

    /**
     * Gets the bill date of the card bill expenditure.
     *
     * @return Bill date of the card bill expenditure.
     */
    public YearMonth getBillDate() {
        return this.billDate;
    }

    /**
     * Gets the category of that the expenditure was tagged to.
     *
     * @return The category of the expenditure that it was tagged to.
     */
    public String getCategory() {
        return this.category;
    }

    /**
     * Checks if it is a + or - to bank amount.
     *
     * @return Plus if adds bank amount, minus if deducts bank amount.
     */
    String checkDebitCredit() {
        if (!this.spent) {
            return "[+] $";
        } else {
            return "[-] $";
        }
    }

    /**
     * Sets the new description of the transaction.
     *
     * @param newDesc New description of transaction.
     */
    void setDescription(String newDesc) {
        this.description = newDesc;
    }

    /**
     * Sets the new amount for the transaction.
     *
     * @param newAmount New amount for the transaction.
     */
    void setAmount(double newAmount) {
        this.amount = newAmount;
    }

    /**
     * Sets the new date of the transaction.
     *
     * @param newDate New date of the transaction.
     */
    public void setDate(Date newDate) {
        this.date = newDate;
    }

    /**
     * Sets the new category of the transaction.
     *
     * @param newCategory New category of the transaction.
     */
    void setCategory(String newCategory) {
        this.category = newCategory;
    }

    /**
     * Gets the card id that the bill belongs to.
     *
     * @return The card id that the bill belongs to.
     */
    public UUID getTransactionCardID() {
        return cardID;
    }

    /**
     * Gets the date of the card bill in YearMonth format.
     *
     * @return Date of the card bill in YearMonth format.
     */
    public YearMonth getTransactionCardBillDate() {
        return billDate;
    }

    /**
     * Gets if this transaction is a card bill.
     *
     * @return True if this transaction is a card bill.
     */
    public boolean isCardBillTransaction() {
        return getTransactionCardID() != null;
    }
}
