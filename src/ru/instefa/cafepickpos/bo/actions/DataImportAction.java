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
package ru.instefa.cafepickpos.bo.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.IOUtils;

import ru.instefa.cafepickpos.Messages;
import ru.instefa.cafepickpos.PosLog;
import ru.instefa.cafepickpos.model.MenuCategory;
import ru.instefa.cafepickpos.model.MenuGroup;
import ru.instefa.cafepickpos.model.MenuItem;
import ru.instefa.cafepickpos.model.MenuItemModifierGroup;
import ru.instefa.cafepickpos.model.MenuModifier;
import ru.instefa.cafepickpos.model.MenuModifierGroup;
import ru.instefa.cafepickpos.model.ModifierMultiplierPrice;
import ru.instefa.cafepickpos.model.Tax;
import ru.instefa.cafepickpos.model.TaxGroup;
import ru.instefa.cafepickpos.model.dao.MenuCategoryDAO;
import ru.instefa.cafepickpos.model.dao.MenuGroupDAO;
import ru.instefa.cafepickpos.model.dao.MenuItemDAO;
import ru.instefa.cafepickpos.model.dao.MenuItemModifierGroupDAO;
import ru.instefa.cafepickpos.model.dao.MenuModifierDAO;
import ru.instefa.cafepickpos.model.dao.MenuModifierGroupDAO;
import ru.instefa.cafepickpos.model.dao.ModifierMultiplierPriceDAO;
import ru.instefa.cafepickpos.ui.dialog.POSMessageDialog;
import ru.instefa.cafepickpos.util.datamigrate.Elements;

public class DataImportAction extends AbstractAction {

	public DataImportAction() {
		super(Messages.getString("DataImportAction.0")); //$NON-NLS-1$
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JFileChooser fileChooser = DataExportAction.getFileChooser();
		int option = fileChooser.showOpenDialog(ru.instefa.cafepickpos.util.POSUtil.getBackOfficeWindow());
		if (option != JFileChooser.APPROVE_OPTION) {
			return;
		}

		File file = fileChooser.getSelectedFile();
		try {

			importMenuItemsFromFile(file);
			POSMessageDialog.showMessage(ru.instefa.cafepickpos.util.POSUtil.getFocusedWindow(), Messages.getString("DataImportAction.1")); //$NON-NLS-1$

		} catch (Exception e1) {
			PosLog.error(getClass(), e1);

			POSMessageDialog.showMessage(ru.instefa.cafepickpos.util.POSUtil.getFocusedWindow(), e1.getMessage());
		}

	}

	public static void importMenuItemsFromFile(File file) throws Exception {
		if (file == null)
			return;

		FileInputStream inputStream = new FileInputStream(file);
		importMenuItems(inputStream);
	}

	public static void importMenuItems(InputStream inputStream) throws Exception {

		Map<String, Object> objectMap = new HashMap<String, Object>();

		try {

			JAXBContext jaxbContext = JAXBContext.newInstance(Elements.class);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			Elements elements = (Elements) unmarshaller.unmarshal(inputStream);

			List<Tax> taxes = elements.getTaxes();
			if (taxes != null) {
				for (Tax tax : taxes) {
					objectMap.put(tax.getUniqueId(), tax);
					tax.setId(1);

					//TaxDAO.getInstance().saveOrUpdate(tax);
				}
			}

			List<MenuCategory> menuCategories = elements.getMenuCategories();
			if (menuCategories != null) {
				for (MenuCategory menuCategory : menuCategories) {

					String uniqueId = menuCategory.getUniqueId();
					objectMap.put(uniqueId, menuCategory);
					menuCategory.setId(null);

					MenuCategoryDAO.getInstance().save(menuCategory);
				}
			}

			List<MenuGroup> menuGroups = elements.getMenuGroups();
			if (menuGroups != null) {
				for (MenuGroup menuGroup : menuGroups) {

					MenuCategory menuCategory = menuGroup.getParent();
					if (menuCategory != null) {
						menuCategory = (MenuCategory) objectMap.get(menuCategory.getUniqueId());
						menuGroup.setParent(menuCategory);
					}

					objectMap.put(menuGroup.getUniqueId(), menuGroup);
					menuGroup.setId(null);

					MenuGroupDAO.getInstance().saveOrUpdate(menuGroup);
				}
			}

			List<MenuModifierGroup> menuModifierGroups = elements.getMenuModifierGroups();
			if (menuModifierGroups != null) {
				for (MenuModifierGroup menuModifierGroup : menuModifierGroups) {
					objectMap.put(menuModifierGroup.getUniqueId(), menuModifierGroup);
					menuModifierGroup.setId(null);

					MenuModifierGroupDAO.getInstance().saveOrUpdate(menuModifierGroup);
				}
			}

			List<MenuModifier> menuModifiers = elements.getMenuModifiers();
			if (menuModifiers != null) {
				for (MenuModifier menuModifier : menuModifiers) {

					objectMap.put(menuModifier.getUniqueId(), menuModifier);
					menuModifier.setId(null);

					// restoring groups
					MenuModifierGroup menuModifierGroup = menuModifier.getModifierGroup();
					if (menuModifierGroup != null) {
						menuModifierGroup = (MenuModifierGroup) objectMap.get(menuModifierGroup.getUniqueId());
						menuModifier.setModifierGroup(menuModifierGroup);
					}

					// restoring taxes
					Tax tax = menuModifier.getTax();
					if (tax != null) {
						tax = (Tax) objectMap.get(tax.getUniqueId());
						menuModifier.setTax(tax);
					}
					
					// multipliers prices to save after menu modifiers will be created
					List<ModifierMultiplierPrice> multiplierPrices = menuModifier.getMultiplierPriceList();
					// modifiers should exist in the database to link them with the multipliers prices
					menuModifier.setMultiplierPriceList(null);

					// creating modifiers
					MenuModifierDAO.getInstance().saveOrUpdate(menuModifier);
					
					// now we can create multipliers prices and their relations with menu modifiers
					if (multiplierPrices != null) {
						for (ModifierMultiplierPrice price : multiplierPrices) {
							if (price.getPrice() != null) {
								price.setId(null);
								price.setModifier(menuModifier);
								ModifierMultiplierPriceDAO.getInstance().saveOrUpdate(price);
							}
						}
					}
				}
			}

			List<MenuItemModifierGroup> menuItemModifierGroups = elements.getMenuItemModifierGroups();
			if (menuItemModifierGroups != null) {
				for (MenuItemModifierGroup mimg : menuItemModifierGroups) {
					objectMap.put(mimg.getUniqueId(), mimg);
					mimg.setId(null);

					MenuModifierGroup menuModifierGroup = mimg.getModifierGroup();
					if (menuModifierGroup != null) {
						menuModifierGroup = (MenuModifierGroup) objectMap.get(menuModifierGroup.getUniqueId());
						mimg.setModifierGroup(menuModifierGroup);
					}

					MenuItemModifierGroupDAO.getInstance().save(mimg);
				}
			}

			List<MenuItem> menuItems = elements.getMenuItems();
			if (menuItems != null) {
				for (MenuItem menuItem : menuItems) {

					objectMap.put(menuItem.getUniqueId(), menuItem);
					menuItem.setId(null);

					MenuGroup menuGroup = menuItem.getParent();
					if (menuGroup != null) {
						menuGroup = (MenuGroup) objectMap.get(menuGroup.getUniqueId());
						menuItem.setParent(menuGroup);
					}

					TaxGroup taxGroup = menuItem.getTaxGroup();
					if (taxGroup != null) {
						taxGroup = (TaxGroup) objectMap.get(taxGroup.getId());
						menuItem.setTaxGroup(taxGroup);
					}

					List<MenuItemModifierGroup> menuItemModiferGroups = menuItem.getMenuItemModiferGroups();
					if (menuItemModiferGroups != null) {
						for (MenuItemModifierGroup menuItemModifierGroup : menuItemModiferGroups) {
							MenuItemModifierGroup menuItemModifierGroup2 = (MenuItemModifierGroup) objectMap.get(menuItemModifierGroup.getUniqueId());
							menuItemModifierGroup.setId(menuItemModifierGroup2.getId());
							menuItemModifierGroup.setModifierGroup(menuItemModifierGroup2.getModifierGroup());
						}
					}

					MenuItemDAO.getInstance().saveOrUpdate(menuItem);
				}
			}

		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}
}
