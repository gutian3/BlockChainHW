pragma solidity ^0.4.24;

import "./Table.sol";

contract Asset {
    string myName;
    int myAmount;
    string key;
    
    constructor () {
        myName = "Bank";
        myAmount = int(1000000);
        key = "receipt";
        createTable();
        insertCompany(myName, int(0));
    }
    // Company
    struct Company {
        string name;
        int money;
        int companyType; // 0 is bank
    }
    Company company;
    // Receipt
    struct Receipt {
        int key;
        string debtorName;
        string debteeName;
        int amount;
        int deadline;
    }
    Receipt receipt;
    function createTable() private {
        TableFactory tf = TableFactory(0x1001);
        tf.createTable("t_company", "name", "companyType,money");
        tf.createTable("t_receipt", "key", "debtorName,debteeName,amount,deadline");
    }
    
    function openTable(string tableName) private returns(Table) {
        TableFactory tf = TableFactory(0x1001);
        Table table = tf.openTable(tableName);
        return table;
    }
    /*
    function byte32ToString(address a) private constant returns (string) {
        bytes memory bytesString = new bytes(32);
        uint charCount = 0;
        for (uint j = 0; j < 32; j++) {
            byte char = byte(bytes32(uint(a) * 2 ** (8 * j)));
            if (char != 0) {
                bytesString[charCount] = char;
                charCount++;
            }
        }
        bytes memory bytesStringTrimmed = new bytes(charCount);
        for (j = 0; j < charCount; j++) {
            bytesStringTrimmed[j] = bytesString[j];
        }
        return string(bytesStringTrimmed);
    }
    */
    function isStringEQ (string a, string b) private returns(bool) {
        if (bytes(a).length != bytes(b).length) {
            return false;
        }
        for(uint i = 0;i < bytes(b).length;i++) {
            if(bytes(a)[i] != bytes(b)[i]) {
                return false;
            }
        }
        return true;
    }
    
    // event about Company Register
    event CompanyRegister (string name);
    event findCompany (string name, int companyType);
    event createReceipt (string debtor, string debtee, int amount, int deadline);
    event findReceipt (string debtor, string debtee, int amount, int deadline);
    event removeR(int count);
    event UpdateR(int count);
    event IsDeadline(string debtor, string debtee, int amount);
    
    // Company Register
    function registerCompany(string name) public returns(string, int){
        Table t_company = openTable("t_company");
        Condition condition = t_company.newCondition();
        Entries entries = t_company.select(name, condition);
        require(entries.size() == 0, "Company should exist and be unique");
        insertCompany(name, int(1));
        emit CompanyRegister(name);
        return (name, int(1));
    }
    
    function selectCompany(string name) public returns(string, int){
        TableFactory tf = TableFactory(0x1001);
        Table table = tf.openTable("t_company");

        Condition condition = table.newCondition();
        Entries entries = table.select(name, condition);
        require(entries.size() == 1, "Company should exist and be unique");
        Entry entry = entries.get(0);
        company.companyType = entry.getInt("companyType");
        company.name = entry.getString("name");
        //company.money = entry.getInt("money");
        emit findCompany(company.name, company.companyType);
        return (name, int(1));
    }
    
    function insertCompany (string name, int companyType) private {
        TableFactory tf = TableFactory(0x1001);
        Table t_company = tf.openTable("t_company");
        //Entries entries = t_company.select(toString(addr), t_company.newCondition());
        //require(entries.size() == 0, "Company should not exist");
        Entry entry = t_company.newEntry();
        entry.set("name", name);
        entry.set("companyType", companyType);
        //entry.set("money", int(10000));
        t_company.insert(name, entry);
    }
    
    function insertReceipt(string debtor, string debtee, int amount, int deadline) public returns(string, string, int, int){
        Table t_receipt = openTable("t_receipt");
        Condition condition1 = t_receipt.newCondition();
        condition1.EQ("debteeName", debtee);
        condition1.EQ("debtorName", debtor);
        Entries entries = t_receipt.select(key, condition1);
        require(entries.size() == 0, "Debtor haven't paid the money debtor owe!");
        Entry entry = t_receipt.newEntry();
        entry.set("debtorName", debtor);
        entry.set("debteeName", debtee);
        entry.set("amount", amount);
        entry.set("deadline", deadline);
        t_receipt.insert(key, entry);
        emit createReceipt(debtor, debtee, amount, deadline);
        return (debtor, debtee, amount, deadline);
    }
    
    
    function removeReceipt(string debtor, string debtee) public returns(int, int) {
        Table t_receipt = openTable("t_receipt");
        Condition condition = t_receipt.newCondition();
        condition.EQ("debteeName", debtee);
        condition.EQ("debtorName", debtor);
        int count = t_receipt.remove(key, condition);
        emit removeR(count);
        return (count, int(0));
    }
    
    function updateReceipt(string debtor, string debtee, int num) public returns(int, int) {
        Table t_receipt = openTable("t_receipt");
        Condition condition = t_receipt.newCondition();
        condition.EQ("debteeName", debtee);
        condition.EQ("debtorName", debtor);
        
        Entry entry = t_receipt.newEntry();
        entry.set("amount", num);

        int count = t_receipt.update(key, entry, condition);
        emit UpdateR(count);
        
        return (count, num);
    }

    function selectReceipt(string debtor, string debtee) public returns(string, string, int, int) {
        Table t_receipt = openTable("t_receipt");
        Condition cond = t_receipt.newCondition();
        cond.EQ("debteeName", debtee);
        Entries entries = t_receipt.select(key, cond);
        require(entries.size() > 0, "receipt id not exists!");
        Entry entry = entries.get(0);
        int j = -1;
        for (int i = 0; i < entries.size(); i++) {
            entry = entries.get(i);
            if (isStringEQ(debtor, entry.getString("debtorName"))) {
                j = i;
                i = entries.size();
            }
        }
        require(j >= 0, "receipt id not exists!");
        entry = entries.get(j);
        receipt.debteeName = entry.getString("debteeName");
        receipt.debtorName = entry.getString("debtorName");
        receipt.amount = entry.getInt("amount");
        receipt.deadline = entry.getInt("deadline");
        emit findReceipt(debtor, debtee, receipt.amount, receipt.deadline);
        return (debtor, debtee, receipt.amount, receipt.deadline);
    }
    
    function computeAll(string name) public returns (int, int) {
        Table t_receipt = openTable("t_receipt");
        Condition cond = t_receipt.newCondition();
        cond.EQ("debteeName", name);
        Entries entries = t_receipt.select(key, cond);
        Condition cond1 = t_receipt.newCondition();
        cond1.EQ("debtorName", name);
        Entries entries1 = t_receipt.select(key, cond1);
        Entry entry = entries.get(0);
        //selectCompany(name);
        int total = 0;
        for (int i = 0; i < entries.size(); i++) {
            entry = entries.get(i);
            total += entry.getInt("amount");
        }
        entry = entries1.get(0);
        for (i = 0; i < entries1.size(); i++) {
            entry = entries1.get(i);
            total -= entry.getInt("amount");
        }
        return (total + 10000, int(0));
    }
    
    function rTransfer(string debtor, string debtee, string toOther, int deadline, int amount1) private {
        Table t_receipt = openTable("t_receipt");
        Condition condition1 = t_receipt.newCondition();
        condition1.EQ("debteeName", debtee);
        condition1.EQ("debtorName", toOther);
        Entries entries1 = t_receipt.select(key, condition1);
        Entry entry = entries1.get(0);
        int amount2 = 0;
        if (entries1.size() == 1) {
            entry = entries1.get(0);
            amount2 = entry.getInt("amount");
            updateReceipt(toOther, debtee, amount1+amount2);
        } else {
            insertReceipt(toOther, debtee, amount1, deadline);
        }
        removeReceipt(debtor, debtee);
        
        Condition condition2 = t_receipt.newCondition();
        condition2.EQ("debteeName", debtor);
        condition2.EQ("debtorName", toOther);
        Entries entries2 = t_receipt.select(key, condition2);
        if (entries2.size() == 1) {
            entry = entries2.get(0);
            amount2 = entry.getInt("amount");
            updateReceipt(toOther, debtor, amount2-amount1);
        } else {
            insertReceipt(toOther, debtor, amount1, deadline);
        }
    }
    // (debtor, debtee) --> (toOther, debtee)
    function rTransferReceipt(string debtor, string debtee, string toOther, int deadline) private {
        Table t_receipt = openTable("t_receipt");
        Condition condition = t_receipt.newCondition();
        condition.EQ("debteeName", debtee);
        condition.EQ("debtorName", debtor);
        Entries entries = t_receipt.select(key, condition);
        require(entries.size() == 1, "The receipt isnot exist!");
        Entry entry = entries.get(0);
        int amount1 = entry.getInt("amount");
        
        rTransfer(debtor,debtee,toOther,deadline,amount1);
        
       //return (int(0), int(0));
    }
    
    function changeR(string debtor, string debtee, string toOther, int deadline) public returns (int, int)
    {
        //rTransferReceipt(debtor, debtee, toOther, deadline);
        return (0, 0);
    }
    
    function timeGo(string debtor, string debtee, int time) returns(int, int){
        Table t_receipt = openTable("t_receipt");
        Condition condition = t_receipt.newCondition();
        condition.EQ("debteeName", debtee);
        condition.EQ("debtorName", debtor);
        Entries entries = t_receipt.select(key, condition);
        require(entries.size() == 1, "The receipt isnot exist!");
        Entry entry = entries.get(0);
        Entry entry1 = t_receipt.newEntry();
        entry1.set("deadline", entry.getInt("deadline") - time);
        if (entry.getInt("deadline") - time <= 0) {
            entry1.set("amount", int(0));
        }
        int count = t_receipt.update(key, entry1, condition);
        emit IsDeadline(debtor, debtee, entry.getInt("amount"));
        return (entry.getInt("deadline") - time, entry.getInt("amount"));
    }
}
