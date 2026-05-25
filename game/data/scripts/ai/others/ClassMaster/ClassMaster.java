/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ai.others.ClassMaster;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerBypass;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLevelChanged;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerPressTutorialMark;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerProfessionChange;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.TutorialCloseHtml;
import org.l2jmobius.gameserver.network.serverpackets.TutorialShowQuestionMark;

/**
 * Class Master AI.
 * @author Nik, Mobius
 */
public class ClassMaster extends Script implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ClassMaster.class.getName());
	
	// NPCs
	private static final List<Integer> CLASS_MASTERS = new ArrayList<>();
	static
	{
		CLASS_MASTERS.add(31756); // Mr. Cat
		CLASS_MASTERS.add(31757); // Miss Queen
	}
	
	// Misc
	private boolean _isEnabled;
	private boolean _spawnClassMasters;
	private boolean _showPopupWindow;
	private final List<ClassChangeData> _classChangeData = new LinkedList<>();
	
	public ClassMaster()
	{
		load();
		addStartNpc(CLASS_MASTERS);
		addTalkId(CLASS_MASTERS);
		addFirstTalkId(CLASS_MASTERS);
	}
	
	@Override
	public void load()
	{
		_classChangeData.clear();
		parseDatapackFile("config/ClassMaster.xml");
		
		if (!_isEnabled)
		{
			return;
		}
		
		if (_spawnClassMasters)
		{
			addSpawn(31756, -116948, 46841, 360, 3355); // Aden
			addSpawn(31756, -84336, 242156, -3725, 24500); // Gludio
			addSpawn(31756, -84119, 243254, -3725, 8000); // Gludio
			addSpawn(31756, -82032, 150160, -3122, 16500); // Gludio
			addSpawn(31756, -44979, -113508, -194, 32000); // Rune
			addSpawn(31756, -14048, 123184, -3115, 32000); // Gludio
			addSpawn(31756, 10968, 17540, -4567, 55000); // Oren
			addSpawn(31756, 11340, 15972, -4577, 14000); // Oren
			addSpawn(31756, 15584, 142784, -2699, 16500); // Dion
			addSpawn(31756, 17956, 170536, -3499, 48000); // Dion
			addSpawn(31756, 44176, -48732, -800, 33000); // Rune
			addSpawn(31756, 44319, -47640, -792, 50000); // Rune
			addSpawn(31756, 45472, 49312, -3067, 53000); // Oren
			addSpawn(31756, 47648, 51296, -2989, 38500); // Oren
			addSpawn(31756, 81136, 54576, -1517, 32000); // Oren
			addSpawn(31756, 83076, 147912, -3467, 32000); // Giran
			addSpawn(31756, 87596, -140674, -1536, 16500); // Schuttgart
			addSpawn(31756, 87824, -142256, -1336, 7342); // Schuttgart
			addSpawn(31756, 110592, 220400, -3667, 0); // Innadril
			addSpawn(31756, 114880, -178144, -827, 0); // Schuttgart
			addSpawn(31756, 116224, -181728, -1373, 0); // Schuttgart
			addSpawn(31756, 117200, 75824, -2725, 25000); // Aden
			addSpawn(31756, 147262, -56450, -2776, 33000); // Goddard
			addSpawn(31756, 147728, 27408, -2198, 16500); // Aden
			addSpawn(31756, 147888, -58048, -2979, 49000); // Goddard
			addSpawn(31756, 148560, -57952, -2974, 53000); // Goddard
			addSpawn(31757, -116902, 46841, 360, 49151); // Aden
			addSpawn(31757, -84294, 242204, -3725, 24500); // Gludio
			addSpawn(31757, -84047, 243193, -3725, 8000); // Gludio
			addSpawn(31757, -81967, 150160, -3122, 16500); // Gludio
			addSpawn(31757, -44983, -113554, -194, 32000); // Rune
			addSpawn(31757, -14050, 123229, -3115, 32000); // Gludio
			addSpawn(31757, 10918, 17511, -4567, 55000); // Oren
			addSpawn(31757, 11353, 16022, -4577, 14000); // Oren
			addSpawn(31757, 15631, 142778, -2699, 16500); // Dion
			addSpawn(31757, 17913, 170536, -3499, 48000); // Dion
			addSpawn(31757, 44176, -48688, -800, 33000); // Rune
			addSpawn(31757, 44294, -47642, -792, 50000); // Rune
			addSpawn(31757, 45414, 49296, -3067, 53000); // Oren
			addSpawn(31757, 47680, 51255, -2989, 38500); // Oren
			addSpawn(31757, 81126, 54519, -1517, 32000); // Oren
			addSpawn(31757, 83082, 147845, -3467, 32000); // Giran
			addSpawn(31757, 87644, -140674, -1536, 16500); // Schuttgart
			addSpawn(31757, 87856, -142272, -1336, 44000); // Schuttgart
			addSpawn(31757, 110592, 220443, -3667, 0); // Innadril
			addSpawn(31757, 114880, -178196, -827, 0); // Schuttgart
			addSpawn(31757, 116218, -181793, -1379, 0); // Schuttgart
			addSpawn(31757, 117160, 75784, -2725, 25000); // Aden
			addSpawn(31757, 147285, -56461, -2776, 11500); // Goddard
			addSpawn(31757, 147761, 27408, -2198, 16500); // Aden
			addSpawn(31757, 147924, -58052, -2979, 49000); // Goddard
			addSpawn(31757, 148514, -57972, -2974, 53000); // Goddard
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _classChangeData.size() + " class change options.");
	}
	
	@Override
	public boolean isValidating()
	{
		return false;
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		NamedNodeMap attrs;
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equals(n.getNodeName()))
			{
				for (Node cm = n.getFirstChild(); cm != null; cm = cm.getNextSibling())
				{
					attrs = cm.getAttributes();
					if ("classMaster".equals(cm.getNodeName()))
					{
						_isEnabled = parseBoolean(attrs, "classChangeEnabled", false);
						if (!_isEnabled)
						{
							return;
						}
						
						_spawnClassMasters = parseBoolean(attrs, "spawnClassMasters", true);
						_showPopupWindow = parseBoolean(attrs, "showPopupWindow", false);
						for (Node c = cm.getFirstChild(); c != null; c = c.getNextSibling())
						{
							attrs = c.getAttributes();
							if ("classChangeOption".equals(c.getNodeName()))
							{
								final List<CategoryType> appliedCategories = new LinkedList<>();
								final List<ItemHolder> requiredItems = new LinkedList<>();
								final List<ItemHolder> rewardedItems = new LinkedList<>();
								boolean setNoble = false;
								boolean setHero = false;
								final String optionName = parseString(attrs, "name", "");
								for (Node b = c.getFirstChild(); b != null; b = b.getNextSibling())
								{
									attrs = b.getAttributes();
									if ("appliesTo".equals(b.getNodeName()))
									{
										for (Node r = b.getFirstChild(); r != null; r = r.getNextSibling())
										{
											attrs = r.getAttributes();
											if ("category".equals(r.getNodeName()))
											{
												final CategoryType category = CategoryType.findByName(r.getTextContent().trim());
												if (category == null)
												{
													LOGGER.severe(getClass().getSimpleName() + ": Incorrect category type: " + r.getNodeValue());
													continue;
												}
												
												appliedCategories.add(category);
											}
										}
									}
									
									if ("rewards".equals(b.getNodeName()))
									{
										for (Node r = b.getFirstChild(); r != null; r = r.getNextSibling())
										{
											attrs = r.getAttributes();
											if ("item".equals(r.getNodeName()))
											{
												final int itemId = parseInteger(attrs, "id");
												final int count = parseInteger(attrs, "count", 1);
												rewardedItems.add(new ItemHolder(itemId, count));
											}
											else if ("setNoble".equals(r.getNodeName()))
											{
												setNoble = true;
											}
											else if ("setHero".equals(r.getNodeName()))
											{
												setHero = true;
											}
										}
									}
									else if ("conditions".equals(b.getNodeName()))
									{
										for (Node r = b.getFirstChild(); r != null; r = r.getNextSibling())
										{
											attrs = r.getAttributes();
											if ("item".equals(r.getNodeName()))
											{
												final int itemId = parseInteger(attrs, "id");
												final int count = parseInteger(attrs, "count", 1);
												requiredItems.add(new ItemHolder(itemId, count));
											}
										}
									}
								}
								
								if (appliedCategories.isEmpty())
								{
									LOGGER.warning(getClass().getSimpleName() + ": Class change option: " + optionName + " has no categories to be applied on. Skipping!");
									continue;
								}
								
								final ClassChangeData classChangeData = new ClassChangeData(optionName, appliedCategories);
								classChangeData.setItemsRequired(requiredItems);
								classChangeData.setItemsRewarded(rewardedItems);
								classChangeData.setRewardHero(setHero);
								classChangeData.setRewardNoblesse(setNoble);
								
								_classChangeData.add(classChangeData);
							}
						}
					}
				}
			}
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "test_server_helper001.html";
	}
	
	@Override
	public String onEvent(String eventValue, Npc npc, Player player)
	{
		if (!_isEnabled)
		{
			return null;
		}
		
		String htmltext = null;
		String event = eventValue;
		final StringTokenizer st = new StringTokenizer(event);
		event = st.nextToken();
		switch (event)
		{
			case "buyitems":
			{
				htmltext = npc.getId() == CLASS_MASTERS.get(0) ? "test_server_helper001a.html" : "test_server_helper001b.html";
				break;
			}
			case "setnoble":
			{
				if (player.isNoble())
				{
					htmltext = "test_server_helper025b.html";
				}
				else if (player.getLevel() < 75)
				{
					htmltext = "test_server_helper025a.html";
				}
				else
				{
					player.setNoble(true);
					player.broadcastUserInfo();
					
					// TODO: SetOneTimeQuestFlag(talker, 10385, 1);
					htmltext = "test_server_helper025.html";
				}
				break;
			}
			case "firstclass":
			{
				htmltext = getFirstOccupationChangeHtml(player);
				break;
			}
			case "secondclass":
			{
				htmltext = getSecondOccupationChangeHtml(player);
				break;
			}
			case "thirdclass":
			{
				if (player.isInCategory(CategoryType.THIRD_CLASS_GROUP) && (player.getLevel() > 75))
				{
					if (changeToNextClass(player))
					{
						player.sendPacket(new PlaySound("ItemSound.quest_fanfare_2"));
						player.broadcastUserInfo();
						htmltext = "test_server_helper021.html";
					}
				}
				else if (player.isInCategory(CategoryType.FOURTH_CLASS_GROUP))
				{
					htmltext = "test_server_helper011.html";
				}
				else
				{
					htmltext = "test_server_helper024.html";
				}
				break;
			}
			case "setclass":
			{
				if (!st.hasMoreTokens())
				{
					return null;
				}
				
				boolean found = false;
				for (ClassChangeData classChangeData : _classChangeData)
				{
					if (classChangeData.isInCategory(player))
					{
						found = true;
					}
				}
				if (!found)
				{
					break;
				}
				
				final int classId = Integer.parseInt(st.nextToken());
				boolean canChange = false;
				if ((player.isInCategory(CategoryType.SECOND_CLASS_GROUP) || player.isInCategory(CategoryType.FIRST_CLASS_GROUP)) && (player.getLevel() >= 40)) // In retail you can skip first occupation
				{
					canChange = CategoryData.getInstance().isInCategory(CategoryType.THIRD_CLASS_GROUP, classId) || (player.isInCategory(CategoryType.FIRST_CLASS_GROUP) && CategoryData.getInstance().isInCategory(CategoryType.SECOND_CLASS_GROUP, classId));
				}
				else if (player.isInCategory(CategoryType.FIRST_CLASS_GROUP) && (player.getLevel() >= 20))
				{
					canChange = CategoryData.getInstance().isInCategory(CategoryType.SECOND_CLASS_GROUP, classId);
				}
				else if (player.isInCategory(CategoryType.THIRD_CLASS_GROUP) && (player.getLevel() >= 76))
				{
					canChange = CategoryData.getInstance().isInCategory(CategoryType.FOURTH_CLASS_GROUP, classId);
				}
				
				if (canChange)
				{
					int classDataIndex = -1;
					if (st.hasMoreTokens())
					{
						classDataIndex = Integer.parseInt(st.nextToken());
					}
					
					if (checkIfClassChangeHasOptions(player) && (classDataIndex == -1))
					{
						htmltext = getHtm(player, "cc_options.html");
						htmltext = htmltext.replace("%name%", ClassListData.getInstance().getClass(classId).getClassName());
						htmltext = htmltext.replace("%options%", getClassChangeOptions(player, classId));
						return htmltext;
					}
					
					final ClassChangeData data = getClassChangeData(classDataIndex);
					if (data != null)
					{
						// Required items.
						if (!data.getItemsRequired().isEmpty())
						{
							for (ItemHolder ri : data.getItemsRequired())
							{
								if (player.getInventory().getInventoryItemCount(ri.getId(), -1) < ri.getCount())
								{
									player.sendMessage("You do not have enough items.");
									return null; // No class change if payment failed.
								}
							}
							
							for (ItemHolder ri : data.getItemsRequired())
							{
								player.destroyItemByItemId(ItemProcessType.FEE, ri.getId(), ri.getCount(), npc, true);
							}
						}
						
						// Give possible rewards.
						if (!data.getItemsRewarded().isEmpty())
						{
							for (ItemHolder ri : data.getItemsRewarded())
							{
								giveItems(player, ri);
							}
						}
						
						// Give possible nobless status reward.
						if (data.isRewardNoblesse())
						{
							player.setNoble(true);
						}
						
						// Give possible hero status reward.
						if (data.isRewardHero())
						{
							player.setHero(true);
						}
					}
					
					player.setPlayerClass(classId);
					if (player.isSubClassActive())
					{
						player.getSubClasses().get(player.getClassIndex()).setPlayerClass(player.getActiveClass());
					}
					else
					{
						player.setBaseClass(player.getActiveClass());
					}
					
					if (PlayerConfig.AUTO_LEARN_SKILLS)
					{
						player.giveAvailableSkills(PlayerConfig.AUTO_LEARN_FS_SKILLS, true, PlayerConfig.AUTO_LEARN_SKILLS_WITHOUT_ITEMS);
					}
					
					player.store(false); // Save player cause if server crashes before this char is saved, he will lose class and the money payed for class change.
					player.broadcastUserInfo();
					player.sendSkillList();
					player.sendPacket(new PlaySound("ItemSound.quest_fanfare_2"));
					return "test_server_helper021.html";
				}
				break;
			}
			case "clanlevel":
			{
				htmltext = player.isClanLeader() ? "test_server_helper022.html" : "pl014.html";
				break;
			}
			case "learnskills":
			{
				player.giveAvailableSkills(true, true, true);
				break;
			}
			case "clanlevelup":
			{
				final Clan clan = player.getClan();
				if ((clan == null) || !player.isClanLeader())
				{
					return null;
				}
				
				if (clan.getLevel() >= 10)
				{
					htmltext = "test_server_helper022a.html";
				}
				else
				{
					clan.changeLevel(clan.getLevel() + 1);
					clan.broadcastClanStatus();
				}
				break;
			}
			case "test_server_helper001.html":
			{
				if (CLASS_MASTERS.contains(npc.getId()))
				{
					htmltext = event;
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	private String getFirstOccupationChangeHtml(Player player)
	{
		String htmltext = null;
		if (player.isInCategory(CategoryType.FIRST_CLASS_GROUP))
		{
			if (player.getLevel() < 20)
			{
				htmltext = "test_server_helper027.html";
			}
			else
			{
				switch (player.getPlayerClass())
				{
					case FIGHTER:
					{
						htmltext = "test_server_helper026a.html";
						break;
					}
					case MAGE:
					{
						htmltext = "test_server_helper026b.html";
						break;
					}
					case ELVEN_FIGHTER:
					{
						htmltext = "test_server_helper026c.html";
						break;
					}
					case ELVEN_MAGE:
					{
						htmltext = "test_server_helper026d.html";
						break;
					}
					case DARK_FIGHTER:
					{
						htmltext = "test_server_helper026e.html";
						break;
					}
					case DARK_MAGE:
					{
						htmltext = "test_server_helper026f.html";
						break;
					}
					case ORC_FIGHTER:
					{
						htmltext = "test_server_helper026g.html";
						break;
					}
					case ORC_MAGE:
					{
						htmltext = "test_server_helper026h.html";
						break;
					}
					case DWARVEN_FIGHTER:
					{
						htmltext = "test_server_helper026i.html";
						break;
					}
					case MALE_SOLDIER:
					{
						htmltext = "test_server_helper026j.html";
						break;
					}
					case FEMALE_SOLDIER:
					{
						htmltext = "test_server_helper026k.html";
						break;
					}
				}
			}
		}
		else if (player.isInCategory(CategoryType.SECOND_CLASS_GROUP))
		{
			htmltext = "test_server_helper028.html";
		}
		else if (player.isInCategory(CategoryType.THIRD_CLASS_GROUP))
		{
			htmltext = "test_server_helper010.html";
		}
		else if (player.isInCategory(CategoryType.FOURTH_CLASS_GROUP))
		{
			htmltext = "test_server_helper011.html";
		}
		
		return htmltext;
	}
	
	private String getSecondOccupationChangeHtml(Player player)
	{
		String htmltext = null;
		if (player.isInCategory(CategoryType.SECOND_CLASS_GROUP) || player.isInCategory(CategoryType.FIRST_CLASS_GROUP))
		{
			if (player.getLevel() < 40)
			{
				htmltext = "test_server_helper023.html";
			}
			else
			{
				switch (player.getPlayerClass())
				{
					case FIGHTER:
					{
						htmltext = "test_server_helper012.html";
						break;
					}
					case WARRIOR:
					{
						htmltext = "test_server_helper012a.html";
						break;
					}
					case KNIGHT:
					{
						htmltext = "test_server_helper012b.html";
						break;
					}
					case ROGUE:
					{
						htmltext = "test_server_helper012c.html";
						break;
					}
					case MAGE:
					{
						htmltext = "test_server_helper013.html";
						break;
					}
					case WIZARD:
					{
						htmltext = "test_server_helper013a.html";
						break;
					}
					case CLERIC:
					{
						htmltext = "test_server_helper013b.html";
						break;
					}
					case ELVEN_FIGHTER:
					{
						htmltext = "test_server_helper014.html";
						break;
					}
					case ELVEN_KNIGHT:
					{
						htmltext = "test_server_helper014a.html";
						break;
					}
					case ELVEN_SCOUT:
					{
						htmltext = "test_server_helper014b.html";
						break;
					}
					case ELVEN_MAGE:
					{
						htmltext = "test_server_helper015.html";
						break;
					}
					case ELVEN_WIZARD:
					{
						htmltext = "test_server_helper015a.html";
						break;
					}
					case ORACLE:
					{
						htmltext = "test_server_helper015b.html";
						break;
					}
					case DARK_FIGHTER:
					{
						htmltext = "test_server_helper016.html";
						break;
					}
					case PALUS_KNIGHT:
					{
						htmltext = "test_server_helper016a.html";
						break;
					}
					case ASSASSIN:
					{
						htmltext = "test_server_helper016b.html";
						break;
					}
					case DARK_MAGE:
					{
						htmltext = "test_server_helper017.html";
						break;
					}
					case DARK_WIZARD:
					{
						htmltext = "test_server_helper017a.html";
						break;
					}
					case SHILLIEN_ORACLE:
					{
						htmltext = "test_server_helper017b.html";
						break;
					}
					case ORC_FIGHTER:
					{
						htmltext = "test_server_helper018.html";
						break;
					}
					case ORC_RAIDER:
					{
						htmltext = "test_server_helper018a.html";
						break;
					}
					case ORC_MONK:
					{
						htmltext = "test_server_helper018b.html";
						break;
					}
					case ORC_MAGE:
					case ORC_SHAMAN:
					{
						htmltext = "test_server_helper019.html";
						break;
					}
					case DWARVEN_FIGHTER:
					{
						htmltext = "test_server_helper020.html";
						break;
					}
					case ARTISAN:
					{
						htmltext = "test_server_helper020b.html";
						break;
					}
					case SCAVENGER:
					{
						htmltext = "test_server_helper020a.html";
						break;
					}
					case MALE_SOLDIER:
					case TROOPER:
					{
						htmltext = "test_server_helper020c.html";
						break;
					}
					case FEMALE_SOLDIER:
					case WARDER:
					{
						htmltext = "test_server_helper020d.html";
						break;
					}
				}
			}
		}
		else if (player.isInCategory(CategoryType.THIRD_CLASS_GROUP))
		{
			htmltext = "test_server_helper010.html";
		}
		else if (player.isInCategory(CategoryType.FOURTH_CLASS_GROUP))
		{
			htmltext = "test_server_helper011.html";
		}
		else
		{
			htmltext = "test_server_helper029.html";
		}
		
		return htmltext;
	}
	
	private boolean changeToNextClass(Player player)
	{
		final PlayerClass newClass = Arrays.stream(PlayerClass.values()).filter(cid -> player.getPlayerClass() == cid.getParent()).findAny().orElse(null);
		if (newClass == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": No new classId found for " + player);
			return false;
		}
		else if (newClass == player.getPlayerClass())
		{
			LOGGER.warning(getClass().getSimpleName() + ": New classId found for " + player + " is exactly the same as the one he currently is!");
			return false;
		}
		else if (checkIfClassChangeHasOptions(player))
		{
			String html = getHtm(player, "cc_options.html");
			html = html.replace("%name%", ClassListData.getInstance().getClass(newClass.getId()).getClassName()); // getEscapedClientCode());
			html = html.replace("%options%", getClassChangeOptions(player, newClass.getId()));
			showResult(player, html);
			return false;
		}
		else
		{
			ClassChangeData data = null;
			for (ClassChangeData ccd : _classChangeData)
			{
				if (ccd.isInCategory(player))
				{
					data = ccd;
					break;
				}
			}
			
			if (data != null)
			{
				// Required items.
				if (!data.getItemsRequired().isEmpty())
				{
					for (ItemHolder ri : data.getItemsRequired())
					{
						if (player.getInventory().getInventoryItemCount(ri.getId(), -1) < ri.getCount())
						{
							player.sendMessage("You do not have enough items.");
							return false; // No class change if payment failed.
						}
					}
					
					for (ItemHolder ri : data.getItemsRequired())
					{
						player.destroyItemByItemId(ItemProcessType.FEE, ri.getId(), ri.getCount(), player, true);
					}
				}
				
				// Give possible rewards.
				if (!data.getItemsRewarded().isEmpty())
				{
					for (ItemHolder ri : data.getItemsRewarded())
					{
						giveItems(player, ri);
					}
				}
				
				// Give possible nobless status reward.
				if (data.isRewardNoblesse())
				{
					player.setNoble(true);
				}
				
				// Give possible hero status reward.
				if (data.isRewardHero())
				{
					player.setHero(true);
				}
			}
			
			player.setPlayerClass(newClass.getId());
			if (player.isSubClassActive())
			{
				player.getSubClasses().get(player.getClassIndex()).setPlayerClass(player.getActiveClass());
			}
			else
			{
				player.setBaseClass(player.getActiveClass());
			}
			
			if (PlayerConfig.AUTO_LEARN_SKILLS)
			{
				player.giveAvailableSkills(PlayerConfig.AUTO_LEARN_FS_SKILLS, true, PlayerConfig.AUTO_LEARN_SKILLS_WITHOUT_ITEMS);
			}
			
			player.store(false); // Save player cause if server crashes before this char is saved, he will lose class and the money payed for class change.
			player.broadcastUserInfo();
			player.sendSkillList();
			return true;
		}
	}
	
	private void showPopupWindow(Player player)
	{
		if (!_showPopupWindow)
		{
			return;
		}
		
		boolean found = false;
		for (ClassChangeData classChangeData : _classChangeData)
		{
			if (classChangeData.isInCategory(player))
			{
				found = true;
			}
		}
		if (!found)
		{
			return;
		}
		
		// @formatter:off
		if ((player.isInCategory(CategoryType.FIRST_CLASS_GROUP) && (player.getLevel() >= 20)) ||
			((player.isInCategory(CategoryType.SECOND_CLASS_GROUP) || player.isInCategory(CategoryType.FIRST_CLASS_GROUP)) && (player.getLevel() >= 40)) ||
			(player.isInCategory(CategoryType.THIRD_CLASS_GROUP) && (player.getLevel() >= 76)))
		// @formatter:on
		{
			player.sendPacket(new TutorialShowQuestionMark(1001));
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_PRESS_TUTORIAL_MARK)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerPressTutorialMark(OnPlayerPressTutorialMark event)
	{
		final Player player = event.getPlayer();
		if (!_showPopupWindow || (event.getMarkId() != 1001))
		{
			return;
		}
		
		boolean found = false;
		for (ClassChangeData classChangeData : _classChangeData)
		{
			if (classChangeData.isInCategory(player))
			{
				found = true;
			}
		}
		if (!found)
		{
			return;
		}
		
		String html = null;
		if (player.isInCategory(CategoryType.FIRST_CLASS_GROUP) && (player.getLevel() >= 20))
		{
			html = getHtm(player, getFirstOccupationChangeHtml(player));
		}
		else if (player.isInCategory(CategoryType.SECOND_CLASS_GROUP) && (player.getLevel() >= 40))
		{
			html = getHtm(player, getSecondOccupationChangeHtml(player));
		}
		else if (player.isInCategory(CategoryType.THIRD_CLASS_GROUP) && (player.getLevel() >= 76))
		{
			html = getHtm(player, "qm_thirdclass.html");
		}
		
		if (html != null)
		{
			showResult(event.getPlayer(), html);
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_BYPASS)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerBypass(OnPlayerBypass event)
	{
		if (event.getCommand().startsWith("Script ClassMaster "))
		{
			final String html = onEvent(event.getCommand().substring(18), null, event.getPlayer());
			event.getPlayer().sendPacket(TutorialCloseHtml.STATIC_PACKET);
			showResult(event.getPlayer(), html);
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_PROFESSION_CHANGE)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerProfessionChange(OnPlayerProfessionChange event)
	{
		showPopupWindow(event.getPlayer());
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LEVEL_CHANGED)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLevelChanged(OnPlayerLevelChanged event)
	{
		showPopupWindow(event.getPlayer());
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogin(OnPlayerLogin event)
	{
		showPopupWindow(event.getPlayer());
	}
	
	private String getClassChangeOptions(Player player, int selectedClassId)
	{
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < _classChangeData.size(); i++)
		{
			final ClassChangeData option = getClassChangeData(i);
			if ((option == null) || option.getCategories().stream().noneMatch(player::isInCategory))
			{
				continue;
			}
			
			sb.append("<tr><td><img src=L2UI_CT1.ChatBalloon_DF_TopCenter width=276 height=1 /></td></tr>");
			sb.append("<tr><td><table bgcolor=3f3f3f width=100%>");
			sb.append("<tr><td align=center><a action=\"bypass -h Script ClassMaster setclass " + selectedClassId + " " + i + "\">" + option.getName() + ":</a></td></tr>");
			sb.append("<tr><td><table width=276>");
			sb.append("<tr><td>Requirements:</td></tr>");
			if (option.getItemsRequired().isEmpty())
			{
				sb.append("<tr><td><font color=LEVEL>Free</font></td></tr>");
			}
			else
			{
				option.getItemsRequired().forEach(ih -> sb.append("<tr><td><font color=\"LEVEL\">" + ih.getCount() + "</font></td><td>" + ItemData.getInstance().getTemplate(ih.getId()).getName() + "</td><td width=30></td></tr>"));
			}
			sb.append("<tr><td>Rewards:</td></tr>");
			if (option.getItemsRewarded().isEmpty())
			{
				if (option.isRewardNoblesse())
				{
					sb.append("<tr><td><font color=\"LEVEL\">Noblesse status.</font></td></tr>");
				}
				
				if (option.isRewardHero())
				{
					sb.append("<tr><td><font color=\"LEVEL\">Hero status.</font></td></tr>");
				}
				
				if (!option.isRewardNoblesse() && !option.isRewardHero())
				{
					sb.append("<tr><td><font color=LEVEL>none</font></td></tr>");
				}
			}
			else
			{
				option.getItemsRewarded().forEach(ih -> sb.append("<tr><td><font color=\"LEVEL\">" + ih.getCount() + "</font></td><td>" + ItemData.getInstance().getTemplate(ih.getId()).getName() + "</td><td width=30></td></tr>"));
				if (option.isRewardNoblesse())
				{
					sb.append("<tr><td><font color=\"LEVEL\">Noblesse status.</font></td></tr>");
				}
				
				if (option.isRewardHero())
				{
					sb.append("<tr><td><font color=\"LEVEL\">Hero status.</font></td></tr>");
				}
			}
			
			sb.append("</table></td></tr>");
			sb.append("</table></td></tr>");
			sb.append("<tr><td><img src=L2UI_CT1.ChatBalloon_DF_TopCenter width=276 height=1 /></td></tr>");
		}
		
		return sb.toString();
	}
	
	private static class ClassChangeData
	{
		private final String _name;
		private final List<CategoryType> _appliedCategories;
		private boolean _rewardNoblesse;
		private boolean _rewardHero;
		private List<ItemHolder> _itemsRequired;
		private List<ItemHolder> _itemsRewarded;
		
		public ClassChangeData(String name, List<CategoryType> appliedCategories)
		{
			_name = name;
			_appliedCategories = appliedCategories != null ? appliedCategories : Collections.emptyList();
		}
		
		public String getName()
		{
			return _name;
		}
		
		public List<CategoryType> getCategories()
		{
			return _appliedCategories != null ? _appliedCategories : Collections.emptyList();
		}
		
		public boolean isInCategory(Player player)
		{
			if (_appliedCategories != null)
			{
				for (CategoryType category : _appliedCategories)
				{
					if (player.isInCategory(category))
					{
						return true;
					}
				}
			}
			
			return false;
		}
		
		public boolean isRewardNoblesse()
		{
			return _rewardNoblesse;
		}
		
		public void setRewardNoblesse(boolean rewardNoblesse)
		{
			_rewardNoblesse = rewardNoblesse;
		}
		
		public boolean isRewardHero()
		{
			return _rewardHero;
		}
		
		public void setRewardHero(boolean rewardHero)
		{
			_rewardHero = rewardHero;
		}
		
		void setItemsRequired(List<ItemHolder> itemsRequired)
		{
			_itemsRequired = itemsRequired;
		}
		
		public List<ItemHolder> getItemsRequired()
		{
			return _itemsRequired != null ? _itemsRequired : Collections.emptyList();
		}
		
		void setItemsRewarded(List<ItemHolder> itemsRewarded)
		{
			_itemsRewarded = itemsRewarded;
		}
		
		public List<ItemHolder> getItemsRewarded()
		{
			return _itemsRewarded != null ? _itemsRewarded : Collections.emptyList();
		}
	}
	
	private boolean checkIfClassChangeHasOptions(Player player)
	{
		boolean showOptions = false;
		
		// Check if there are requirements.
		for (ClassChangeData ccd : _classChangeData)
		{
			if (!ccd.getItemsRequired().isEmpty() && ccd.isInCategory(player))
			{
				showOptions = true;
				break;
			}
		}
		
		if (!showOptions)
		{
			// Check if there is more than 1 reward to chose.
			int count = 0;
			for (ClassChangeData ccd : _classChangeData)
			{
				if (!ccd.getItemsRewarded().isEmpty() && ccd.isInCategory(player))
				{
					count++;
				}
			}
			
			if (count > 1)
			{
				showOptions = true;
			}
		}
		
		return showOptions;
	}
	
	private ClassChangeData getClassChangeData(int index)
	{
		if ((index >= 0) && (index < _classChangeData.size()))
		{
			return _classChangeData.get(index);
		}
		
		return null;
	}
	
	public static void main(String[] args)
	{
		new ClassMaster();
	}
}
