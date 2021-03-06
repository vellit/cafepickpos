/**
 * ************************************************************************
 * * The contents of this file are subject to the MRPL 1.2
 * * (the  "License"),  being   the  Mozilla   Public  License
 * * Version 1.1  with a permitted attribution clause; you may not  use this
 * * file except in compliance with the License. You  may  obtain  a copy of
 * * the License at http://www.floreantpos.org/license.html
 * * Software distributed under the License  is  distributed  on  an "AS IS"
 * * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * * License for the specific  language  governing  rights  and  limitations
 * * under the License.
 * * The Original Code is FLOREANT POS.
 * * The Initial Developer of the Original Code is OROCUBE LLC
 * * All portions are Copyright (C) 2015 OROCUBE LLC
 * * All Rights Reserved.
 * ************************************************************************
 */
package ru.instefa.cafepickpos.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;

import ru.instefa.cafepickpos.Messages;
import ru.instefa.cafepickpos.POSConstants;
import ru.instefa.cafepickpos.PosLog;
import ru.instefa.cafepickpos.bo.actions.DataImportAction;
import ru.instefa.cafepickpos.config.TerminalConfig;
import ru.instefa.cafepickpos.exceptions.DatabaseConnectionException;
import ru.instefa.cafepickpos.model.CashDrawer;
import ru.instefa.cafepickpos.model.Currency;
import ru.instefa.cafepickpos.model.Discount;
import ru.instefa.cafepickpos.model.MenuItemSize;
import ru.instefa.cafepickpos.model.Multiplier;
import ru.instefa.cafepickpos.model.OrderType;
import ru.instefa.cafepickpos.model.PizzaCrust;
import ru.instefa.cafepickpos.model.PosTransaction;
import ru.instefa.cafepickpos.model.Restaurant;
import ru.instefa.cafepickpos.model.Shift;
import ru.instefa.cafepickpos.model.Tax;
import ru.instefa.cafepickpos.model.Terminal;
import ru.instefa.cafepickpos.model.Ticket;
import ru.instefa.cafepickpos.model.User;
import ru.instefa.cafepickpos.model.UserPermission;
import ru.instefa.cafepickpos.model.UserType;
import ru.instefa.cafepickpos.model.dao.CurrencyDAO;
import ru.instefa.cafepickpos.model.dao.DiscountDAO;
import ru.instefa.cafepickpos.model.dao.MenuItemSizeDAO;
import ru.instefa.cafepickpos.model.dao.MultiplierDAO;
import ru.instefa.cafepickpos.model.dao.OrderTypeDAO;
import ru.instefa.cafepickpos.model.dao.PizzaCrustDAO;
import ru.instefa.cafepickpos.model.dao.PosTransactionDAO;
import ru.instefa.cafepickpos.model.dao.RestaurantDAO;
import ru.instefa.cafepickpos.model.dao.ShiftDAO;
import ru.instefa.cafepickpos.model.dao.TaxDAO;
import ru.instefa.cafepickpos.model.dao.TerminalDAO;
import ru.instefa.cafepickpos.model.dao.TicketDAO;
import ru.instefa.cafepickpos.model.dao.UserDAO;
import ru.instefa.cafepickpos.model.dao.UserTypeDAO;
import ru.instefa.cafepickpos.model.dao._RootDAO;

public class DatabaseUtil {
	private static Log logger = LogFactory.getLog(DatabaseUtil.class);

	public static void checkConnection(String connectionString, String hibernateDialect, String hibernateConnectionDriverClass, String user, String password)
			throws DatabaseConnectionException {
		Configuration configuration = _RootDAO.getNewConfiguration(null);

		configuration = configuration.setProperty("hibernate.dialect", hibernateDialect);
		configuration = configuration.setProperty("hibernate.connection.driver_class", hibernateConnectionDriverClass);

		configuration = configuration.setProperty("hibernate.connection.url", connectionString);
		configuration = configuration.setProperty("hibernate.connection.username", user);
		configuration = configuration.setProperty("hibernate.connection.password", password);

		checkConnection(configuration);
	}

	public static void checkConnection() throws DatabaseConnectionException {
		Configuration configuration = _RootDAO.getNewConfiguration(null);
		checkConnection(configuration);
	}

	public static void checkConnection(Configuration configuration) throws DatabaseConnectionException {
		try {
			SessionFactory sessionFactory = configuration.buildSessionFactory();
			Session session = sessionFactory.openSession();
			Transaction transaction = session.beginTransaction();
			transaction.rollback();
			session.close();
		} catch (Exception e) {
			throw new DatabaseConnectionException(e);
		}
	}

	public static boolean createDatabase(String connectionString, String hibernateDialect, String hibernateConnectionDriverClass, String user, String password,
			boolean exportSampleData) {
		try {
			Configuration configuration = _RootDAO.getNewConfiguration(null);

			configuration = configuration.setProperty("hibernate.dialect", hibernateDialect);
			configuration = configuration.setProperty("hibernate.connection.driver_class", hibernateConnectionDriverClass);

			configuration = configuration.setProperty("hibernate.connection.url", connectionString);
			configuration = configuration.setProperty("hibernate.connection.username", user);
			configuration = configuration.setProperty("hibernate.connection.password", password);
			configuration = configuration.setProperty("hibernate.hbm2ddl.auto", "create");
			configuration = configuration.setProperty("hibernate.c3p0.checkoutTimeout", "0"); //$NON-NLS-1$ //$NON-NLS-2$

			SchemaExport schemaExport = new SchemaExport(configuration);
			schemaExport.create(true, true);

			_RootDAO.initialize();

			Restaurant restaurant = new Restaurant();
			restaurant.setId(Integer.valueOf(1));
			restaurant.setName(Messages.getString("DatabaseUtil.0"));
			restaurant.setAddressLine1(Messages.getString("DatabaseUtil.1"));
			restaurant.setTelephone(Messages.getString("DatabaseUtil.2"));
			RestaurantDAO.getInstance().saveOrUpdate(restaurant);

			Tax tax = new Tax();
			tax.setName(Messages.getString("DatabaseUtil.3"));
			tax.setRate(Double.valueOf(6));
			TaxDAO.getInstance().saveOrUpdate(tax);

			Shift shift = new Shift();
			shift.setName(ru.instefa.cafepickpos.POSConstants.GENERAL);
			java.util.Date shiftStartTime = ShiftUtil.buildShiftStartTime(0, 0, 0, 11, 59, 1);
			java.util.Date shiftEndTime = ShiftUtil.buildShiftEndTime(0, 0, 0, 11, 59, 1);

			shift.setStartTime(shiftStartTime);
			shift.setEndTime(shiftEndTime);
			long length = Math.abs(shiftStartTime.getTime() - shiftEndTime.getTime());

			shift.setShiftLength(Long.valueOf(length));
			ShiftDAO.getInstance().saveOrUpdate(shift);

			UserType administrator = new UserType();
			administrator.setName(ru.instefa.cafepickpos.POSConstants.ADMINISTRATOR);
			administrator.setPermissions(new HashSet<UserPermission>(Arrays.asList(UserPermission.permissions)));
			UserTypeDAO.getInstance().saveOrUpdate(administrator);

			UserType manager = new UserType();
			manager.setName(ru.instefa.cafepickpos.POSConstants.MANAGER);
			manager.setPermissions(new HashSet<UserPermission>(Arrays.asList(UserPermission.permissions)));
			UserTypeDAO.getInstance().saveOrUpdate(manager);

			UserType cashier = new UserType();
			cashier.setName(ru.instefa.cafepickpos.POSConstants.CASHIER);
			cashier.setPermissions(new HashSet<UserPermission>(Arrays.asList(UserPermission.CREATE_TICKET, UserPermission.SETTLE_TICKET,
					UserPermission.SPLIT_TICKET, UserPermission.VIEW_ALL_OPEN_TICKETS)));
			UserTypeDAO.getInstance().saveOrUpdate(cashier);

			UserType server = new UserType();
			server.setName(Messages.getString("DatabaseUtil.4"));
			server.setPermissions(new HashSet<UserPermission>(Arrays.asList(UserPermission.CREATE_TICKET, UserPermission.SETTLE_TICKET,
					UserPermission.SPLIT_TICKET)));
			//server.setTest(Arrays.asList(OrderType.BAR_TAB));
			UserTypeDAO.getInstance().saveOrUpdate(server);

			User administratorUser = new User();
			administratorUser.setUserId(123);
			administratorUser.setSsn("123");
			administratorUser.setPassword("1111");
			administratorUser.setFirstName(Messages.getString("DatabaseUtil.5"));
			administratorUser.setLastName(Messages.getString("DatabaseUtil.6"));
			administratorUser.setType(administrator);
			administratorUser.setActive(true);

			UserDAO dao = new UserDAO();
			dao.saveOrUpdate(administratorUser);

			User managerUser = new User();
			managerUser.setUserId(124);
			managerUser.setSsn("124");
			managerUser.setPassword("2222");
			managerUser.setFirstName(Messages.getString("DatabaseUtil.7"));
			managerUser.setLastName(Messages.getString("DatabaseUtil.8"));
			managerUser.setType(manager);
			managerUser.setActive(true);

			dao.saveOrUpdate(managerUser);

			User cashierUser = new User();
			cashierUser.setUserId(125);
			cashierUser.setSsn("125");
			cashierUser.setPassword("3333");
			cashierUser.setFirstName(Messages.getString("DatabaseUtil.9"));
			cashierUser.setLastName(Messages.getString("DatabaseUtil.10"));
			cashierUser.setType(cashier);
			cashierUser.setActive(true);

			dao.saveOrUpdate(cashierUser);

			User serverUser = new User();
			serverUser.setUserId(126);
			serverUser.setSsn("126");
			serverUser.setPassword("7777");
			serverUser.setFirstName(Messages.getString("DatabaseUtil.11"));
			serverUser.setLastName(Messages.getString("DatabaseUtil.12"));
			serverUser.setType(server);
			serverUser.setActive(true);

			dao.saveOrUpdate(serverUser);

			User driverUser = new User();
			driverUser.setUserId(127);
			driverUser.setSsn("127");
			driverUser.setPassword("8888");
			driverUser.setFirstName(Messages.getString("DatabaseUtil.13"));
			driverUser.setLastName(Messages.getString("DatabaseUtil.14"));
			driverUser.setType(server);
			driverUser.setDriver(true);
			driverUser.setActive(true);

			dao.saveOrUpdate(driverUser);

			OrderTypeDAO orderTypeDAO = new OrderTypeDAO();
			OrderType orderType = new OrderType();
			orderType.setName(POSConstants.DINE_IN_BUTTON_TEXT);
			orderType.setShowTableSelection(true);
			orderType.setCloseOnPaid(true);
			orderType.setEnabled(true);
			orderType.setShouldPrintToKitchen(true);
			orderType.setShowInLoginScreen(true);
			orderTypeDAO.save(orderType);

			orderType = new OrderType();
			orderType.setName(POSConstants.TAKE_OUT_BUTTON_TEXT);
			orderType.setShowTableSelection(false);
			orderType.setCloseOnPaid(true);
			orderType.setEnabled(true);
			orderType.setPrepaid(true);
			orderType.setShouldPrintToKitchen(true);
			orderType.setShowInLoginScreen(true);
			orderTypeDAO.save(orderType);

			orderType = new OrderType();
			orderType.setName(POSConstants.RETAIL_BUTTON_TEXT);
			orderType.setShowTableSelection(false);
			orderType.setCloseOnPaid(true);
			orderType.setEnabled(true);
			orderType.setShouldPrintToKitchen(false);
			orderType.setShowInLoginScreen(true);
			orderTypeDAO.save(orderType);

			orderType = new OrderType();
			orderType.setName(POSConstants.HOME_DELIVERY_BUTTON_TEXT);
			orderType.setShowTableSelection(false);
			orderType.setCloseOnPaid(false);
			orderType.setEnabled(true);
			orderType.setShouldPrintToKitchen(true);
			orderType.setShowInLoginScreen(true);
			orderType.setRequiredCustomerData(true);
			orderType.setDelivery(true);
			orderTypeDAO.save(orderType);

			DiscountDAO discountDao = new DiscountDAO();

			Discount discount1 = new Discount();
			discount1.setName(Messages.getString("DatabaseUtil.15"));
			discount1.setType(1);
			discount1.setValue(100.0);
			discount1.setAutoApply(false);
			discount1.setMinimunBuy(2);
			discount1.setQualificationType(0);
			discount1.setApplyToAll(true);
			discount1.setNeverExpire(true);
			discount1.setEnabled(true);

			discountDao.saveOrUpdate(discount1);

			Discount discount2 = new Discount();
			discount2.setName(Messages.getString("DatabaseUtil.16"));
			discount2.setType(1);
			discount2.setValue(100.0);
			discount2.setAutoApply(true);
			discount2.setMinimunBuy(3);
			discount2.setQualificationType(0);
			discount2.setApplyToAll(true);
			discount2.setNeverExpire(true);
			discount2.setEnabled(true);

			discountDao.saveOrUpdate(discount2);

			Discount discount3 = new Discount();
			discount3.setName(Messages.getString("DatabaseUtil.17"));
			discount3.setType(1);
			discount3.setValue(10.0);
			discount3.setAutoApply(false);
			discount3.setMinimunBuy(1);
			discount3.setQualificationType(0);
			discount3.setApplyToAll(true);
			discount3.setNeverExpire(true);
			discount3.setEnabled(true);
			discountDao.saveOrUpdate(discount3);

			int terminalId = TerminalConfig.getTerminalId();

			if (terminalId == -1) {
				Random random = new Random();
				terminalId = random.nextInt(10000) + 1;
			}
			Terminal terminal = new Terminal();
			terminal.setId(terminalId);
			terminal.setName(String.valueOf(terminalId));

			TerminalDAO.getInstance().saveOrUpdate(terminal);

			CashDrawer cashDrawer = new CashDrawer();
			cashDrawer.setTerminal(terminal);

			Currency currency = new Currency();
			currency.setName(Messages.getString("DatabaseUtil.18"));
			currency.setCode(Messages.getString("DatabaseUtil.31"));
			currency.setSymbol(Messages.getString("DatabaseUtil.19"));
			currency.setExchangeRate(1.0);
			currency.setMain(true);
			CurrencyDAO.getInstance().save(currency);

			MenuItemSize menuItemSize = new MenuItemSize();
			menuItemSize.setName(Messages.getString("DatabaseUtil.20"));
			menuItemSize.setSortOrder(0);
			MenuItemSizeDAO.getInstance().save(menuItemSize);

			menuItemSize = new MenuItemSize();
			menuItemSize.setName(Messages.getString("DatabaseUtil.21"));
			menuItemSize.setSortOrder(1);
			MenuItemSizeDAO.getInstance().save(menuItemSize);

			menuItemSize = new MenuItemSize();
			menuItemSize.setName(Messages.getString("DatabaseUtil.22"));
			menuItemSize.setSortOrder(2);
			MenuItemSizeDAO.getInstance().save(menuItemSize);

			PizzaCrust crust = new PizzaCrust();
			crust.setName(Messages.getString("DatabaseUtil.23"));
			crust.setSortOrder(0);
			PizzaCrustDAO.getInstance().save(crust);

			crust = new PizzaCrust();
			crust.setName(Messages.getString("DatabaseUtil.24"));
			crust.setSortOrder(1);
			PizzaCrustDAO.getInstance().save(crust);

			Multiplier multiplier = new Multiplier("Regular");
			multiplier.setRate(0.0);
			multiplier.setSortOrder(0);
			multiplier.setTicketPrefix(Messages.getString("Multiplier.0"));
			multiplier.setDefaultMultiplier(true);
			multiplier.setMain(true);
			MultiplierDAO.getInstance().save(multiplier);

			multiplier = new Multiplier("No");
			multiplier.setRate(0.0);
			multiplier.setSortOrder(1);
			multiplier.setTicketPrefix(Messages.getString("DatabaseUtil.25"));
			multiplier.setDefaultMultiplier(false);
			MultiplierDAO.getInstance().save(multiplier);

			multiplier = new Multiplier("Half");
			multiplier.setRate(50.0);
			multiplier.setSortOrder(2);
			multiplier.setTicketPrefix(Messages.getString("DatabaseUtil.26"));
			multiplier.setDefaultMultiplier(false);
			MultiplierDAO.getInstance().save(multiplier);

			multiplier = new Multiplier("Quarter");
			multiplier.setRate(25.0);
			multiplier.setSortOrder(3);
			multiplier.setTicketPrefix(Messages.getString("DatabaseUtil.27"));
			multiplier.setDefaultMultiplier(false);
			MultiplierDAO.getInstance().save(multiplier);

			multiplier = new Multiplier("Extra");
			multiplier.setRate(200.0);
			multiplier.setSortOrder(4);
			multiplier.setTicketPrefix(Messages.getString("DatabaseUtil.28"));
			multiplier.setDefaultMultiplier(false);
			MultiplierDAO.getInstance().save(multiplier);

			multiplier = new Multiplier("Triple");
			multiplier.setRate(300.0);
			multiplier.setSortOrder(5);
			multiplier.setTicketPrefix(Messages.getString("DatabaseUtil.29"));
			multiplier.setDefaultMultiplier(false);
			MultiplierDAO.getInstance().save(multiplier);

			multiplier = new Multiplier("Sub");
			multiplier.setRate(100.0);
			multiplier.setSortOrder(6);
			multiplier.setTicketPrefix(Messages.getString("DatabaseUtil.30"));
			multiplier.setDefaultMultiplier(false);
			MultiplierDAO.getInstance().save(multiplier);

			if (!exportSampleData) {
				return true;
			}

			DataImportAction.importMenuItems(DatabaseUtil.class.getResourceAsStream(POSUtil.getMenuFilename()));

			return true;
		} catch (Exception e) {
			PosLog.error(DatabaseUtil.class, e.getMessage());
			logger.error(e);
			return false;
		}
	}

	public static boolean updateDatabase(String connectionString, String hibernateDialect, String hibernateConnectionDriverClass, String user, String password) {
		try {
			Configuration configuration = _RootDAO.getNewConfiguration(null);

			configuration = configuration.setProperty("hibernate.dialect", hibernateDialect);
			configuration = configuration.setProperty("hibernate.connection.driver_class", hibernateConnectionDriverClass);

			configuration = configuration.setProperty("hibernate.connection.url", connectionString);
			configuration = configuration.setProperty("hibernate.connection.username", user);
			configuration = configuration.setProperty("hibernate.connection.password", password);
			configuration = configuration.setProperty("hibernate.hbm2ddl.auto", "update");

			SchemaUpdate schemaUpdate = new SchemaUpdate(configuration);
			schemaUpdate.execute(true, true);

			_RootDAO.initialize();

			return true;
		} catch (Exception e) {
			PosLog.error(DatabaseUtil.class, e.getMessage());
			logger.error(e);
			return false;
		}
	}

	public static Configuration initialize() throws DatabaseConnectionException {
		try {
			return _RootDAO.reInitialize();
		} catch (Exception e) {
			logger.error(e);
			throw new DatabaseConnectionException(e);
		}

	}

	public static void main(String[] args) throws Exception {
		initialize();

		List<PosTransaction> findAll = PosTransactionDAO.getInstance().findAll();
		for (PosTransaction posTransaction : findAll) {
			PosTransactionDAO.getInstance().delete(posTransaction);
		}

		List<Ticket> list = TicketDAO.getInstance().findAll();
		for (Ticket ticket : list) {
			TicketDAO.getInstance().delete(ticket);
		}
	}
}
