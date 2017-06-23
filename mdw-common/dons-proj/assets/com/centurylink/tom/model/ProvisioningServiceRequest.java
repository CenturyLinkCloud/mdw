package com.centurylink.tom.model;

import com.centurylink.mdw.model.Jsonable;
import java.util.List;
import org.json.JSONObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description="ProvisioningServiceRequest Object to create Service Order")
public class ProvisioningServiceRequest implements Jsonable
{

    private String sourceSystem;
    
    private String salesChannel;
    @ApiModelProperty(required=true, value="Unique transaction id")
    private String transactionId;
    private String orderDate;
    private String timestamp;
    private String customerServiceOrderType;
    private String csrId;
    @ApiModelProperty(required=true, value="Service Order Version Number")
    private String version;
    private String customerServiceOrderNumber;
    @ApiModelProperty(required=true, value="List of CSR Notes")
    private List<CsrNote> csrNote;
    
   /**
     * @param json
     * 
     */
    public ProvisioningServiceRequest(JSONObject json) {
         bind(json);  
    }

    /**
     * 
     * @param customerOrderItems
     * @param timestamp
     * @param csrId
     * @param schedule
     * @param customerServiceOrderNumber
     * @param transactionId
     * @param sourceSystem
     * @param csrNote
     * @param customerServiceOrderType
     * @param orderDate
     * @param relatedParty
     * @aram salesChannel
     * @param customerInfo
     * @param version
     */
    public ProvisioningServiceRequest(String sourceSystem, String salesChannel, String transactionId, String orderDate, String timestamp, String customerServiceOrderType, String csrId, String version, String customerServiceOrderNumber, List<CsrNote> csrNote) {
        this.sourceSystem = sourceSystem;
        this.salesChannel = salesChannel;
        this.transactionId = transactionId;
        this.orderDate = orderDate;
        this.timestamp = timestamp;
        this.customerServiceOrderType = customerServiceOrderType;
        this.csrId = csrId;
        this.version = version;
        this.customerServiceOrderNumber = customerServiceOrderNumber;
        this.csrNote = csrNote;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getSalesChannel() {
        return salesChannel;
    }

    public void setSalesChannel(String salesChannel) {
        this.salesChannel = salesChannel;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getCustomerServiceOrderType() {
        return customerServiceOrderType;
    }

    public void setCustomerServiceOrderType(String customerServiceOrderType) {
        this.customerServiceOrderType = customerServiceOrderType;
    }

    public String getCsrId() {
        return csrId;
    }

    public void setCsrId(String csrId) {
        this.csrId = csrId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCustomerServiceOrderNumber() {
        return customerServiceOrderNumber;
    }

    public void setCustomerServiceOrderNumber(String customerServiceOrderNumber) {
        this.customerServiceOrderNumber = customerServiceOrderNumber;
    }

    public List<CsrNote> getCsrNote() {
        return csrNote;
    }

    public void setCsrNote(List<CsrNote> csrNote) {
        this.csrNote = csrNote;
    }

}
