## **API Endpoint Specifications**

To enable core protection features, the Bank will provide MoneyGuard with access to the following five endpoints:

### **I. Customer Identity Verification**

* **Purpose:** Validates customer identity using a customerId provided by the bank for each user.  
* **Endpoint:** POST /api/MoneyGuard/CustomerDetails/{customerId}  
* **Request Parameters:** customerId.  
* **Response Body:** name, email, phone.

### **II. Customer & Account Lookup**

* **Purpose:** Fetches all active bank accounts associated with a customer during onboarding.  
* **Endpoint:** GET /api/MoneyGuard/GetActiveAccountsByCustomerID/{customerId}  
* **Request Parameters:** customerId.  
* **Response Body:** Returns a list of account objects including name, NUBAN, and accountType.

### **III. Premium Collection (Debit)**

* **Purpose:** Processes automated subscription payments for MoneyGuard coverage.  
* **Endpoint:** POST /api/MoneyGuard/DebitMoneyGuardPremium  
* **Request Body:** customerId, accountNumber, amount.  
* **Response Body:** IsSuccessful, errorMessage.

### **IV. Risk Monitoring (Transaction History)**

* **Purpose:** Analyzes transaction patterns for real-time risk scoring.  
* **Endpoint:** GET /api/MoneyGuard/GetTransactions  
* **Request Parameters:** customerId, accountNumber, fromDate, toDate.  
* **Response Body:** A list of transaction records for the specified period.

### **V. Fraud Prevention (Post-No-Debit)**

* **Purpose:** Instantly restricts an account if high-confidence fraud is detected.  
* **Endpoint:** POST /api/MoneyGuard/ActivatePND  
* **Request Body:** customerId, accountNo.  
* **Response Body:** IsSuccessful, errorMessage.

**Detailed Spec:**

**I. Customer Identity Verification**

**Purpose:** Validates the customer's identity using a unique ID provided by the bank during the onboarding phase.

• **Endpoint:** `POST /api/MoneyGuard/CustomerDetails/{customerId}`

• **Path Parameter:**

    ◦ `customerId` (string)

• **Expected Response Body:**

*{*  
  *"isError": false,*  
  *"errorCode": null,*  
  *"message": "Customer details retrieved successfully",*  
  *"data": {*  
    *"id": "19",*  
    *"firstName": "Adekunle",*  
    *"lastName": "Ciroma",*  
    *"bvn": "209438678",*  
    *"email": "adekunlechukwuemeka@gmail.com",*  
    *"telephone": "+2348023616918"*  
  *}*  
*}*

**II. Customer & Account Lookup**

**Purpose:** Fetches all active bank accounts associated with a customer during the MoneyGuard onboarding process.

• **Endpoint:** `GET /api/MoneyGuard/GetActiveAccountsByCustomerID/{customerId}`

• **Path Parameter:**

    ◦ `customerId` (string)

• **Expected Response Body:**

*{*  
  *"isError": false,*  
  *"errorCode": null,*  
  *"message": "Active accounts retrieved successfully",*  
  *"data": {*  
    *"accounts": \[*  
      *{*  
        *"nuban": "0123456789",*  
        *"name": "Adekunle Ciroma Chukwuemeka",*  
        *"type": "Savings"*  
      *}*  
    *\]*  
  *}*  
*}*

III. Premium Collection (Debit)

**Purpose:** Processes the automated subscription payments required for MoneyGuard coverage.

• **Endpoint:** `POST /api/MoneyGuard/DebitMoneyGuardPremium`

• **Expected Request Body:**

• *(Note: The request body requires the customerId, accountNumber, and amount.)*

• **Expected Response Body:**

*{*  
  *"isError": false,*  
  *"errorCode": null,*  
  *"message": "Premium debited successfully",*  
  *"data": {*  
    *"transactionId": "DEBIT\_af123mn657",*  
    *"status": "COMPLETED"*  
  *}*  
*}*

IV. Risk Monitoring (Transaction History)

**Purpose:** Analyzes the user's transaction patterns for real-time risk scoring.

• **Endpoint:** `GET /api/MoneyGuard/GetTransactions`

• **Query Parameters:**

    ◦ `customerId` (string)

    ◦ `accountNumber` (string)

    ◦ `fromDate` (string/date)

    ◦ `toDate` (string/date)

• **Expected Response Body:**

*{*  
  *"isError": false,*  
  *"errorCode": null,*  
  *"message": "Transactions retrieved successfully",*  
  *"data": {*  
    *"transactions": \[*  
      *{*  
        *"id": "12345678",*  
        *"amount": "1000000",*  
        *"timestamp": "2023-10-27T14:30:00Z",*  
        *"memo": "Moneyguard quarterly subscription",*  
        *"isCredit": true*  
      *}*  
    *\]*  
  *}*  
*}*

V. Fraud Prevention (Post-No-Debit)

**Purpose:** Instantly restricts a user's account if high-confidence fraud is detected.

• **Endpoint:** `POST /api/MoneyGuard/ActivatePND`

• **Expected Request Body:**

• *(Note: The request body requires customerId and accountNo.)*

• **Expected Response Body:**

*{*  
  *"isError": false,*  
  *"errorCode": null,*  
  *"message": "Post-No-Debit activated successfully",*  
  *"data": {*  
    *"accountNo": "0123456789",*  
    *"status": "RESTRICTED",*  
    *"actionTimestamp": "2023-10-28T09:15:00Z"*  
  *}*  
*}*  
