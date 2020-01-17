package org.fisco.bcos.asset.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.fisco.bcos.asset.contract.Asset;
//import org.fisco.bcos.asset.contract.Asset.RegisterEventEventResponse;
//import org.fisco.bcos.asset.contract.Asset.TransferEventEventResponse;
import org.fisco.bcos.channel.client.Service;
import org.fisco.bcos.web3j.crypto.Credentials;
import org.fisco.bcos.web3j.crypto.Keys;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.protocol.channel.ChannelEthereumService;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.fisco.bcos.web3j.tuples.generated.Tuple4;
import org.fisco.bcos.web3j.tuples.generated.Tuple3;
import org.fisco.bcos.web3j.tuples.generated.Tuple2;
import org.fisco.bcos.web3j.tuples.generated.Tuple1;
import org.fisco.bcos.web3j.tx.gas.StaticGasProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class AssetClient extends JFrame{

	static Logger logger = LoggerFactory.getLogger(AssetClient.class);

	private Web3j web3j;

	private Credentials credentials;

	private String debtor;
    private String debtee;
    private String toOther;
    private BigInteger amount;
    private BigInteger companyType;
    private BigInteger deadline;

	public Web3j getWeb3j() {
		return web3j;
	}

	public void setWeb3j(Web3j web3j) {
		this.web3j = web3j;
	}

	public Credentials getCredentials() {
		return credentials;
	}

	public void setCredentials(Credentials credentials) {
		this.credentials = credentials;
	}

	public void recordAssetAddr(String address) throws FileNotFoundException, IOException {
		Properties prop = new Properties();
		prop.setProperty("address", address);
		final Resource contractResource = new ClassPathResource("contract.properties");
		FileOutputStream fileOutputStream = new FileOutputStream(contractResource.getFile());
		prop.store(fileOutputStream, "contract address");
	}

	public String loadAssetAddr() throws Exception {
		// load Asset contact address from contract.properties
		Properties prop = new Properties();
		final Resource contractResource = new ClassPathResource("contract.properties");
		prop.load(contractResource.getInputStream());

		String contractAddress = prop.getProperty("address");
		if (contractAddress == null || contractAddress.trim().equals("")) {
			throw new Exception(" load Asset contract address failed, please deploy it first. ");
		}
		logger.info(" load Asset address from contract.properties, address is {}", contractAddress);
		return contractAddress;
	}

	public void initialize() throws Exception {

		// init the Service
		@SuppressWarnings("resource")
		ApplicationContext context = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
		Service service = context.getBean(Service.class);
		service.run();

		ChannelEthereumService channelEthereumService = new ChannelEthereumService();
		channelEthereumService.setChannelService(service);
		Web3j web3j = Web3j.build(channelEthereumService, 1);

		// init Credentials
		Credentials credentials = Credentials.create(Keys.createEcKeyPair());

		setCredentials(credentials);
		setWeb3j(web3j);

		logger.debug(" web3j is " + web3j + " ,credentials is " + credentials);
	}

	private static BigInteger gasPrice = new BigInteger("30000000");
	private static BigInteger gasLimit = new BigInteger("30000000");

	public void deployAssetAndRecordAddr() {

		try {
			Asset asset = Asset.deploy(web3j, credentials, new StaticGasProvider(gasPrice, gasLimit)).send();
			System.out.println(" deploy Asset success, contract address is " + asset.getContractAddress());

			recordAssetAddr(asset.getContractAddress());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			System.out.println(" deploy Asset contract failed, error message is  " + e.getMessage());
		}
	}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public boolean queryCompany(String companyName) {
		try {
			String contractAddress = loadAssetAddr();

			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			
			TransactionReceipt receipt = asset.selectCompany(companyName).send();
			Tuple1<String> result = asset.getSelectCompanyInput(receipt);
			//if (result.getValue1().compareTo(new BigInteger("0")) == 0) {
			System.out.printf(" company name %s \n", result.getValue1());
			//} else {
			//System.out.printf(" %s asset account is not exist \n", assetAccount);
			//}
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			logger.error(" queryCompany exception, error message is {}", e.getMessage());

			System.out.printf(" query company failed, error message is %s\n", e.getMessage());
			return false;
		}
	}

	public boolean registerCompany(String assetAccount/*, BigInteger amount*/) {
		try {
			String contractAddress = loadAssetAddr();

			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			
			TransactionReceipt receipt = asset.registerCompany(assetAccount).send();
			Tuple2<String, BigInteger> result = asset.getRegisterCompanyOutput(receipt);
			//if (!response.isEmpty()) {
			//	if (response.get(0).ret.compareTo(new BigInteger("0")) == 0) {
			System.out.printf(" register company success => company: %s \n", assetAccount);
			return true;
			//	} else {
			//System.out.printf(" register asset account failed, ret code is %s \n",
			//				response.get(0).ret.toString());
			//	}
			//} else {
			//System.out.println(" event log not found, maybe transaction not exec. ");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			logger.error(" registerCompany exception, error message is {}", e.getMessage());
			System.out.printf(" register company failed, error message is %s\n", e.getMessage());
			return false;
		}
	}

	public boolean updateReceipt(String debtor, String debtee, BigInteger num) {
		try {
			String contractAddress = loadAssetAddr();

			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			
			TransactionReceipt receipt = asset.updateReceipt(debtor, debtee, num).send();
			Tuple2<BigInteger, BigInteger> result = asset.getUpdateReceiptOutput(receipt);
			if (result.getValue1().compareTo(new BigInteger("1")) == 0){
				System.out.printf(" update receipt success => new amount: %s\n", result.getValue2());
				return true;
			} else {
				System.out.printf(" update receipt failed or the receipt isn't exist.\n");
				return false;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			logger.error(" updateReceipt exception, error message is {}", e.getMessage());
			System.out.printf(" update receipt failed, error message is %s\n", e.getMessage());
			return false;
		}
	}

	public boolean computeAllR(String name) {
		try {
			String contractAddress = loadAssetAddr();

			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			
			TransactionReceipt receipt = asset.computeAll(name).send();
			Tuple2<BigInteger, BigInteger> result = asset.getComputeAllOutput(receipt);
			this.amount = result.getValue1();
			System.out.printf(" compute all receipt of %s success => total: %s \n", name, result.getValue1());
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			logger.error(" computeAll exception, error message is {}", e.getMessage());
			System.out.printf(" compute all receipt failed, error message is %s\n", e.getMessage());
			return false;
		}
	}

	public boolean queryReceipt(String debtor, String debtee) {
		try {
			String contractAddress = loadAssetAddr();

			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			
			TransactionReceipt receipt = asset.selectReceipt(debtor, debtee).send();
			Tuple4<String, String, BigInteger, BigInteger> result = asset.getSelectReceiptOutput(receipt);
			this.debtor = result.getValue1();
			this.debtee = result.getValue2();
			this.amount = result.getValue3();
			this.deadline = result.getValue4();
			System.out.printf(" query receipt success => debtor: %s, debtee: %s, amount: %s, deadline: %s \n", 
				result.getValue1(), result.getValue2(), result.getValue3(), result.getValue4());
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			logger.error(" selectReceipt exception, error message is {}", e.getMessage());
			System.out.printf(" query receipt failed, error message is %s\n", e.getMessage());
			return false;
		}
	}

	public boolean skipTime(String debtor, String debtee, BigInteger time) {
		try {
			String contractAddress = loadAssetAddr();

			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			
			TransactionReceipt receipt = asset.timeGo(debtor, debtee, time).send();
			Tuple2<BigInteger, BigInteger> result = asset.getTimeGoOutput(receipt);

			System.out.printf(" skip time success => new amount: %s, new deadline: %s \n", 
				result.getValue1(), result.getValue2());
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			logger.error(" timeGo exception, error message is {}\n", e.getMessage());
			System.out.printf(" skip time failed, error message is %s\n", e.getMessage());
			return false;
		}
	}

	public boolean deleteReceipt(String debtor, String debtee) {
		try {
			String contractAddress = loadAssetAddr();

			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			
			TransactionReceipt receipt = asset.removeReceipt(debtor, debtee).send();
			Tuple2<BigInteger, BigInteger> result = asset.getRemoveReceiptOutput(receipt);
			if (result.getValue1().compareTo(new BigInteger("1")) == 0){
				System.out.printf(" delete receipt success\n");
				return true;
			} else {
				System.out.printf(" delete receipt failed or the receipt isn't exist.\n");
				return false;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			logger.error(" removeReceipt exception, error message is {}", e.getMessage());
			System.out.printf(" delete receipt failed, error message is %s\n", e.getMessage());
			return false;
		}
	}

	public boolean createReceipt(String debtor, String debtee, BigInteger amount, BigInteger deadline) {
		try {
			String contractAddress = loadAssetAddr();

			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			
			TransactionReceipt receipt = asset.insertReceipt(debtor, debtee, amount, deadline).send();
			Tuple4<String, String, BigInteger, BigInteger> result = asset.getInsertReceiptOutput(receipt);

			System.out.printf(" create receipt success => debtor: %s, debtee: %s, amount: %s, deadline: %s.\n",
				result.getValue1(), result.getValue2(), result.getValue3(), result.getValue4());
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			logger.error(" insertReceipt exception, error message is {}", e.getMessage());
			System.out.printf(" create receipt failed, error message is %s\n", e.getMessage());
			return false;
		}
	}



	public boolean transferReceipt(String debtor, String debtee, String toOther, BigInteger deadline) {
		try {
			String contractAddress = loadAssetAddr();
			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = asset.changeR(debtor, debtee, toOther, deadline).send();
			Tuple2<BigInteger, BigInteger> response = asset.getChangeROutput(receipt);
			System.out.printf(" create receipt success ");
			this.debtor = debtor;
			this.debtee = debtee;
			this.toOther = toOther;
			this.deadline = deadline;
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			logger.error(" changeR exception, error message is {}", e.getMessage());
			System.out.printf(" transfer receipt failed, error message is %s\n", e.getMessage());
			return false;
		}
	}

	public static void Usage() {
		System.out.println(" Usage:");
		System.out.println("\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient deploy");
		System.out.println("\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient query account");
		System.out.println(
				"\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient register account value");
		System.out.println(
				"\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient transfer from_account to_account amount");
		System.exit(0);
	}

	private static final long serialVersionUID = 1L;
    JTable table;
    JTable table2;
    private JTable table3;
    private DefaultTableModel tableModel;
    private DefaultTableModel tableModel2;
	private DefaultTableModel tableModel3;
    private JTextField aTextField;
    private JTextField bTextField;
    private JTextField cTextField;
    private JTextField dTextField;
    private JTextField eTextField;
    private JTextField TextField1;
    private JTextField TextField2;
    private JTextField TextField3;
    private JTextField TextField4;
    private boolean flag;


	public static void main(String[] args) throws Exception {

		if (args.length < 1) {
			Usage();
			System.out.printf(" 1 ");
		}

		AssetClient client = new AssetClient();
		client.initialize();
		client.setVisible(true);

		switch (args[0]) {
		case "deploy":
			client.deployAssetAndRecordAddr();
			break;
		case "queryCompany":
			if (args.length < 2) {
				Usage();
				System.out.printf(" 2 ");
			}
			client.queryCompany(args[1]);
			break;
		case "registerCompany":
			if (args.length < 2) {
				Usage();
				System.out.printf(" 3 ");
			}
			client.registerCompany(args[1]);
			break;
		case "computeAllR":
			if (args.length < 2) {
				Usage();
				System.out.printf(" 4 ");
			}
			client.computeAllR(args[1]);
			break;
		case "updateReceipt":
			if (args.length < 4) {
				Usage();
				System.out.printf(" 5 ");
			}
			client.updateReceipt(args[1], args[2], new BigInteger(args[3]));
			break;
		case "queryReceipt":
			if (args.length < 3) {
				Usage();
				System.out.printf(" 6 ");
			}
			client.queryReceipt(args[1], args[2]);
			break;
		case "skipTime":
			if (args.length < 4) {
				Usage();
				System.out.printf(" 7 ");
			}
			client.skipTime(args[1], args[2], new BigInteger(args[3]));
			break;
		case "deleteReceipt":
			if (args.length < 3) {
				Usage();
				System.out.printf(" 8 ");
			}
			client.deleteReceipt(args[1], args[2]);
			break;
		case "createReceipt":
			if (args.length < 5) {
				Usage();
				System.out.printf(" 9 ");
			}
			client.createReceipt(args[1], args[2], new BigInteger(args[3]), new BigInteger(args[4]));
			break;
		case "transferReceipt":
			if (args.length < 5) {
				Usage();
				System.out.printf(" 10 ");
			}
			client.transferReceipt(args[1], args[2], args[3], new BigInteger(args[4]));
			break;
		default: {
			Usage();
			System.out.printf(" 11 ");
		}
		}

		//System.exit(0);
	}


	public AssetClient() {
		super();
        setTitle("bill");
        setBounds(100,100,1500,900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        String[] columnNames = { "debtor", "debtee", "amount", "deadline"};
        String [][]tableVales={};
        String[] columnNames2 = {"company name", "money", "companyType"};
        String [][]tableVales2={{"Bank","", "0"}};
        String[] columnNames3 = { "query result", "result", "result", "result", "result"};
        String [][]tableVales3 = {};
        tableModel = new DefaultTableModel(tableVales,columnNames);
        tableModel2 = new DefaultTableModel(tableVales2, columnNames2);
        tableModel3 = new DefaultTableModel(tableVales3,columnNames3);
        table = new JTable(tableModel);
        table2 = new JTable(tableModel2);
        table3 = new JTable(tableModel3);
        table.setAlignmentX(CENTER_ALIGNMENT);
        table.setRowHeight(30);
        table2.setRowHeight(30);
        table3.setRowHeight(30);
        JTableHeader head = table.getTableHeader();
        head.setPreferredSize(new Dimension(head.getWidth(), 50));
        head.setFont(new Font("Serief", Font.ITALIC + Font.BOLD, 20));
        head = table2.getTableHeader();
        head.setPreferredSize(new Dimension(head.getWidth(), 50));
        head.setFont(new Font("Serief", Font.ITALIC + Font.BOLD, 20));
        head = table3.getTableHeader();
        head.setPreferredSize(new Dimension(head.getWidth(), 50));
        head.setFont(new Font("Serief", Font.ITALIC + Font.BOLD, 20));
        JScrollPane scrollPane = new JScrollPane(table);
        getContentPane().add(scrollPane, BorderLayout.WEST);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setTableStyle(table);
        JScrollPane scrollPane2 = new JScrollPane(table2);
        getContentPane().add(scrollPane2, BorderLayout.EAST);
        table2.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setTableStyle(table2);
        JScrollPane scrollPane3 = new JScrollPane(table3);
        getContentPane().add(scrollPane3, BorderLayout.SOUTH);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setTableStyle(table3);
        table.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){
                int selectedRow = table.getSelectedRow();
                Object oa = tableModel.getValueAt(selectedRow, 0);
                Object ob = tableModel.getValueAt(selectedRow, 1);
                Object oc = tableModel.getValueAt(selectedRow, 2);
                Object od = tableModel.getValueAt(selectedRow, 3);
                aTextField.setText(oa.toString());
                bTextField.setText(ob.toString());
                cTextField.setText(oc.toString());
                dTextField.setText(od.toString());
            }
        });
        
        table2.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){
                int selectedRow = table2.getSelectedRow();
                Object oa = tableModel2.getValueAt(selectedRow, 0);
                eTextField.setText(oa.toString());
            }
        });
        
        scrollPane.setViewportView(table);
        final JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(1500, 80));
        getContentPane().add(panel,BorderLayout.CENTER);
        
        String debtor = "debtor: ";
		String debtee = "debtee: ";
		String amount = "amount: ";
		String deadline = "deadline: ";
		String transfer = "transfer to:";
		String companyName = "company: ";
		JLabel L1 = new JLabel(debtor, JLabel.CENTER);
		JLabel L11 = new JLabel(debtor, JLabel.CENTER);
		JLabel L2 = new JLabel(debtee, JLabel.CENTER);
		JLabel L22 = new JLabel(debtee, JLabel.CENTER);
		JLabel L3 = new JLabel(amount, JLabel.CENTER);
		JLabel L5 = new JLabel(deadline, JLabel.CENTER);
		JLabel L55 = new JLabel(deadline, JLabel.CENTER);
		JLabel L6 = new JLabel(companyName, JLabel.CENTER);
		JLabel L4 = new JLabel(transfer, JLabel.CENTER);
		
		Font fnt = new Font("Serief", Font.ITALIC + Font.BOLD, 20);
		L1.setFont(fnt);
		L2.setFont(fnt);
		L11.setFont(fnt);
		L22.setFont(fnt);
		L3.setFont(fnt);
		L5.setFont(fnt);
		L55.setFont(fnt);
		L4.setFont(fnt);
		L6.setFont(fnt);
		table.setFont(fnt);
		table2.setFont(fnt);
		table3.setFont(fnt);
		
        panel.add(L1);
        aTextField = new JTextField("name",10);
        aTextField.setFont(fnt);
        panel.add(aTextField);
        panel.add(L2);
        bTextField = new JTextField("name",10);
        bTextField.setFont(fnt);
        panel.add(bTextField);
        panel.add(L3);
        cTextField = new JTextField("0",10);
        cTextField.setFont(fnt);
        panel.add(cTextField);
        panel.add(L5);
        dTextField = new JTextField("0",10);
        dTextField.setFont(fnt);
        panel.add(dTextField);
        
        flag = false;
        final JButton addButton = new JButton("add");
        addButton.setPreferredSize(new Dimension(160, 50));
        addButton.setFont(fnt);
        addButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                String []rowValues = {aTextField.getText(), 
                	bTextField.getText(), cTextField.getText(), dTextField.getText()};
                flag = createReceipt(aTextField.getText(), bTextField.getText(), 
                	new BigInteger(cTextField.getText()), new BigInteger(dTextField.getText()));
                if (flag) {
                	tableModel.addRow(rowValues);
                //int rowCount = table.getRowCount() +1;
                	aTextField.setText("");
                	bTextField.setText("");
                	cTextField.setText("");
                	dTextField.setText("");
                }
            }
        });
        panel.add(addButton);  

        final JButton updateButton = new JButton("update");
        updateButton.setPreferredSize(new Dimension(160, 50));
        updateButton.setFont(fnt);
        updateButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                int selectedRow = table.getSelectedRow();
                if(selectedRow!= -1)
                {
                	flag = updateReceipt((String)tableModel.getValueAt(selectedRow, 0), 
                		(String)tableModel.getValueAt(selectedRow, 1), new BigInteger(cTextField.getText()));
                    
                    //tableModel.setValueAt(aTextField.getText(), selectedRow, 0);
                   // tableModel.setValueAt(bTextField.getText(), selectedRow, 1);
                	if (flag)
                    	tableModel.setValueAt(cTextField.getText(), selectedRow, 2);
                    //tableModel.setValueAt(dTextField.getText(), selectedRow, 3);
                    //table.setValueAt(arg0, arg1, arg2)
                }
            }
        });
        panel.add(updateButton);

        final JButton delButton = new JButton("delete");
        delButton.setPreferredSize(new Dimension(160, 50));
        delButton.setFont(fnt);
        delButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                int selectedRow = table.getSelectedRow();
                if(selectedRow!=-1)
                {
                	flag = deleteReceipt((String)tableModel.getValueAt(selectedRow, 0), 
                		(String)tableModel.getValueAt(selectedRow, 1));
                	if (flag) 
                    	tableModel.removeRow(selectedRow);
                }
            }
        });
        panel.add(delButton);
        
        panel.add(L6);
        eTextField = new JTextField("Bank",10);
        eTextField.setFont(fnt);
        panel.add(eTextField);
        
        final JButton registerButton = new JButton("register");
        //registerButton.setPreferredSize(new Dimension(150, 50));
        registerButton.setFont(fnt);
        registerButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
            	flag = registerCompany(eTextField.getText());
            		if (flag) {
                	String []rowValues = {eTextField.getText(),"","1"};
              		tableModel2.addRow(rowValues);
               	 	//int rowCount = table2.getRowCount() +1;
                	eTextField.setText("");
            	}
            }
        });
        panel.add(registerButton);
        
        final JButton qButton = new JButton("findCompany");
        //registerButton.setPreferredSize(new Dimension(150, 50));
        qButton.setFont(fnt);
        qButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
            	flag = queryCompany(eTextField.getText());
            	if (flag) {
                String []rowValues = {eTextField.getText(), "", ""};
                tableModel3.addRow(rowValues);
                //int rowCount = table3.getRowCount() +1;
                eTextField.setText("");
            }
            }
        });
        panel.add(qButton);
        
        panel.add(L11);
        TextField1 = new JTextField("name",10);
        TextField1.setFont(fnt);
        panel.add(TextField1);
        panel.add(L22);
        TextField2 = new JTextField("name",10);
        TextField2.setFont(fnt);
        panel.add(TextField2);
        
        final JButton queryButton = new JButton("query the receipt");
        //queryButton.setPreferredSize(new Dimension(150, 50));
        queryButton.setFont(fnt);
        queryButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
            	flag = queryReceipt(TextField1.getText(), TextField2.getText());
            	if (flag) {
                String []rowValues = {TextField1.getText(),TextField2.getText(), amount, deadline};
                tableModel3.addRow(rowValues);
                //int rowCount = table.getRowCount() +1;
                TextField1.setText("");
                TextField2.setText("");
            }
            }
        });
        panel.add(queryButton);
        
        final JButton computerButton = new JButton("computer");
        //registerButton.setPreferredSize(new Dimension(150, 50));
        computerButton.setFont(fnt);
        computerButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
            	int selectedRow = table2.getSelectedRow();
                if(selectedRow!= -1)
                {
                	flag = computeAllR((String)tableModel2.getValueAt(selectedRow, 0));
                    if (flag) {
                    //tableModel2.setValueAt(aTextField.getText(), selectedRow, 0);
                    tableModel2.setValueAt(amount, selectedRow, 1);
                    //table.setValueAt(arg0, arg1, arg2)
                }
                }
            }
        });
        panel.add(computerButton);
        
        final JButton tranButton = new JButton("tranfer");
        tranButton.setFont(fnt);
        tranButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                int selectedRow = table2.getSelectedRow();
                if(0 == ((String)tableModel2.getValueAt(selectedRow, 0)).compareTo("Bank"))
                {
                	flag = transferReceipt(TextField1.getText(), TextField2.getText(),
                	 TextField4.getText(), new BigInteger(TextField3.getText()));
                	if (flag) {

                	}
                }
            }
        });
        panel.add(tranButton);
        
        panel.add(L4);
        TextField4 = new JTextField("name",10);
        TextField4.setFont(fnt);
        panel.add(TextField4);
        
        panel.add(L55);
        TextField3 = new JTextField("0",10);
        TextField3.setFont(fnt);
        panel.add(TextField3);
        
        final JButton clearButton = new JButton("clear results");
        clearButton.setPreferredSize(new Dimension(500, 50));
        clearButton.setFont(fnt);
        clearButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                int rowCount = table3.getRowCount();
                for (int i = 0; i < rowCount; i++) {
                	tableModel3.removeRow(i);
                }
            }
        });
        panel.add(clearButton);
	}

	@SuppressWarnings("static-access")
	public static void setTableStyle(JTable jtb) {
        jtb.setSelectionBackground(new Color(224, 242, 255));
        
        jtb.setRowHeight(40);
        
        jtb.setAutoCreateRowSorter(true);
        
        DefaultTableCellRenderer  renderer = (DefaultTableCellRenderer) jtb.getTableHeader().getDefaultRenderer();
        renderer.setHorizontalAlignment(renderer.CENTER);
        renderer.setFont(new Font("Serief", Font.ITALIC + Font.BOLD, 20));
        
        DefaultTableCellRenderer r=new DefaultTableCellRenderer();
        r.setHorizontalAlignment(JLabel.CENTER);
        jtb.setDefaultRenderer(Object.class,r);

        jtb.setFocusable(false);
        fitTableColumns(jtb);
    }
	
	@SuppressWarnings("rawtypes")
    private static void fitTableColumns(JTable myTable)
    {
         myTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
         JTableHeader header = myTable.getTableHeader();
         int rowCount = myTable.getRowCount();
         Enumeration columns = myTable.getColumnModel().getColumns();
         while(columns.hasMoreElements())
         {
             TableColumn column = (TableColumn)columns.nextElement();
             int col = header.getColumnModel().getColumnIndex(column.getIdentifier());
             int width = (int)header.getDefaultRenderer().getTableCellRendererComponent
             (myTable, column.getIdentifier(), false, false, -1, col).getPreferredSize().getWidth();
             for(int row = 0; row < rowCount; row++)
             {
                 int preferedWidth = (int)myTable.getCellRenderer(row, col).getTableCellRendererComponent
                 (myTable, myTable.getValueAt(row, col), false, false, row, col).getPreferredSize().getWidth();
                 width = Math.max(width, preferedWidth);
             }
             header.setResizingColumn(column);
             column.setWidth(width+myTable.getIntercellSpacing().width);
         }
    }
}
