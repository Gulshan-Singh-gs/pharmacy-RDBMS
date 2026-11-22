package main;

import java.math.BigDecimal;
import java.sql.Date;

/**
 * Drug - model representing a drug in the inventory.
 *
 * @author Gulshan Singh
 */
public class Drug {

    private int drugId;
    private String name;
    private String category;
    private int companyId;
    private String batchNo;
    private int quantity;
    private BigDecimal buyingPrice;
    private BigDecimal sellingPrice;
    private Date expiryDate;

    public Drug() {
    }

    public Drug(int drugId, String name, String category, int companyId,
                String batchNo, int quantity, BigDecimal buyingPrice,
                BigDecimal sellingPrice, Date expiryDate) {
        this.drugId = drugId;
        this.name = name;
        this.category = category;
        this.companyId = companyId;
        this.batchNo = batchNo;
        this.quantity = quantity;
        this.buyingPrice = buyingPrice;
        this.sellingPrice = sellingPrice;
        this.expiryDate = expiryDate;
    }

    public int getDrugId() { return drugId; }
    public void setDrugId(int drugId) { this.drugId = drugId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getCompanyId() { return companyId; }
    public void setCompanyId(int companyId) { this.companyId = companyId; }

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getBuyingPrice() { return buyingPrice; }
    public void setBuyingPrice(BigDecimal buyingPrice) { this.buyingPrice = buyingPrice; }

    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }

    public Date getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Date expiryDate) { this.expiryDate = expiryDate; }
}
