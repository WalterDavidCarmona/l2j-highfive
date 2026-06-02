package org.l2jmobius.gameserver.data;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.custom.SchemeBufferConfig;
import org.l2jmobius.gameserver.model.actor.holders.npc.BuffSkillHolder;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class SchemeBufferTable
{
	private static final Logger LOGGER = Logger.getLogger(SchemeBufferTable.class.getName());
	private static final String SKILLS_XML_PATH = "./data/SchemeBufferSkills.xml";
	private static final String LOAD_SCHEMES = "SELECT * FROM buffer_schemes";
	private static final String DELETE_SCHEMES = "TRUNCATE TABLE buffer_schemes";
	private static final String INSERT_SCHEME = "INSERT INTO buffer_schemes (object_id, scheme_name, skills) VALUES (?,?,?)";
	private static final String TYPE_MAGE_GROUP = "MAGE_GROUP";
	private static final String TYPE_FIGHTER_GROUP = "FIGHTER_GROUP";
	private static final String DB_COLUMN_OBJECT_ID = "object_id";
	private static final String DB_COLUMN_SCHEME_NAME = "scheme_name";
	private static final String DB_COLUMN_SKILLS = "skills";
	private static final String SKILL_SEPARATOR = ",";
	private final Map<Integer, Map<String, List<Integer>>> _schemesTable = new ConcurrentHashMap<>();
	private final Map<Integer, BuffSkillHolder> _availableBuffs = new LinkedHashMap<>();
	private final Map<String, List<Integer>> _skillIdsByType;
	private final Map<String, Map<Integer, BuffSkillHolder>> _availableBuffsByType;
	private final List<String> _skillTypesOrder;

	public SchemeBufferTable()
	{
		_skillIdsByType = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		_availableBuffsByType = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		_skillTypesOrder = new ArrayList<>();
		loadAvailableBuffs();
		loadPlayerSchemes();
		LOGGER.info("SchemeBufferTable: Loaded " + _schemesTable.size() + " players and " + _availableBuffs.size() + " available skills.");
	}

	/**
	 * Recarga los skills disponibles desde SchemeBufferSkills.xml en tiempo real.
	 * Solo usar desde comandos de admin o bypass GM.
	 */
	public synchronized void reloadAvailableBuffs()
	{
		_availableBuffs.clear();
		_availableBuffsByType.clear();
		_skillIdsByType.clear();
		_skillTypesOrder.clear();
		loadAvailableBuffs();
		LOGGER.info("SchemeBufferTable: Skills recargados desde " + SKILLS_XML_PATH);
	}

	public void saveSchemes()
	{
		try
		{
			Connection connection = DatabaseFactory.getConnection();

			try
			{
				PreparedStatement deleteStatement = connection.prepareStatement("TRUNCATE TABLE buffer_schemes");

				try
				{
					deleteStatement.execute();
				}
				catch (Throwable var13)
				{
					if (deleteStatement != null)
					{
						try
						{
							deleteStatement.close();
						}
						catch (Throwable var12)
						{
							var13.addSuppressed(var12);
						}
					}

					throw var13;
				}

				if (deleteStatement != null)
				{
					deleteStatement.close();
				}

				deleteStatement = connection.prepareStatement("INSERT INTO buffer_schemes (object_id, scheme_name, skills) VALUES (?,?,?)");

				try
				{
					for (Map.Entry<Integer, Map<String, List<Integer>>> playerEntry : _schemesTable.entrySet())
					{
						for (Map.Entry<String, List<Integer>> schemeEntry : playerEntry.getValue().entrySet())
						{
							StringBuilder skillsBuilder = new StringBuilder();

							for (int skillId : schemeEntry.getValue())
							{
								skillsBuilder.append(skillId).append(",");
							}

							if (skillsBuilder.length() > 0)
							{
								skillsBuilder.setLength(skillsBuilder.length() - ",".length());
							}

							deleteStatement.setInt(1, playerEntry.getKey());
							deleteStatement.setString(2, schemeEntry.getKey());
							deleteStatement.setString(3, skillsBuilder.toString());
							deleteStatement.addBatch();
						}
					}

					deleteStatement.executeBatch();
				}
				catch (Throwable var14)
				{
					if (deleteStatement != null)
					{
						try
						{
							deleteStatement.close();
						}
						catch (Throwable var11)
						{
							var14.addSuppressed(var11);
						}
					}

					throw var14;
				}

				if (deleteStatement != null)
				{
					deleteStatement.close();
				}
			}
			catch (Throwable var15)
			{
				if (connection != null)
				{
					try
					{
						connection.close();
					}
					catch (Throwable var10)
					{
						var15.addSuppressed(var10);
					}
				}

				throw var15;
			}

			if (connection != null)
			{
				connection.close();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "SchemeBufferTable: Error while saving schemes. Players: " + _schemesTable.size() + ".", e);
		}
	}

	public void setScheme(int playerId, String schemeName, List<Integer> skillIds)
	{
		Map<String, List<Integer>> schemes = _schemesTable.computeIfAbsent(playerId, (key) -> new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER));
		if (schemes.size() < SchemeBufferConfig.BUFFER_MAX_SCHEMES || schemes.containsKey(schemeName))
		{
			List<Integer> safeList = skillIds != null ? Collections.unmodifiableList(new ArrayList<>(skillIds)) : Collections.emptyList();
			schemes.put(schemeName, safeList);
		}
	}

	public boolean deleteScheme(int playerId, String schemeName)
	{
		Map<String, List<Integer>> schemes = _schemesTable.get(playerId);
		if (schemes == null)
		{
			return false;
		}

		boolean removed = schemes.remove(schemeName) != null;
		if (schemes.isEmpty())
		{
			_schemesTable.remove(playerId);
		}

		return removed;
	}

	public boolean addSkillToScheme(int playerId, String schemeName, int skillId)
	{
		Map<String, List<Integer>> schemes = _schemesTable.get(playerId);
		if (schemes == null)
		{
			return false;
		}

		List<Integer> current = schemes.get(schemeName);
		if (current == null)
		{
			return false;
		}

		if (current.contains(skillId))
		{
			return false;
		}

		List<Integer> updated = new ArrayList<>(current);
		updated.add(skillId);
		schemes.put(schemeName, Collections.unmodifiableList(updated));
		return true;
	}

	public boolean removeSkillFromScheme(int playerId, String schemeName, int skillId)
	{
		Map<String, List<Integer>> schemes = _schemesTable.get(playerId);
		if (schemes == null)
		{
			return false;
		}

		List<Integer> current = schemes.get(schemeName);
		if (current == null)
		{
			return false;
		}

		if (!current.contains(skillId))
		{
			return false;
		}

		List<Integer> updated = new ArrayList<>(current);
		updated.remove((Object) skillId);
		schemes.put(schemeName, Collections.unmodifiableList(updated));
		return true;
	}

	public Map<String, List<Integer>> getPlayerSchemes(int playerId)
	{
		Map<String, List<Integer>> schemes = _schemesTable.get(playerId);
		return schemes != null ? Collections.unmodifiableMap(schemes) : null;
	}

	public List<Integer> getScheme(int playerId, String schemeName)
	{
		Map<String, List<Integer>> schemes = _schemesTable.get(playerId);
		return schemes != null && schemes.get(schemeName) != null ? schemes.get(schemeName) : Collections.emptyList();
	}

	public List<Integer> getSkillsIdsByType(String groupType)
	{
		return _skillIdsByType.getOrDefault(groupType, Collections.emptyList());
	}

	public List<String> getSkillTypes()
	{
		List<String> skillTypes = new ArrayList<>(_skillTypesOrder.size());

		for (String type : _skillTypesOrder)
		{
			if (!type.equalsIgnoreCase("MAGE_GROUP") && !type.equalsIgnoreCase("FIGHTER_GROUP"))
			{
				skillTypes.add(type);
			}
		}

		return skillTypes;
	}

	public BuffSkillHolder getAvailableBuff(int skillId)
	{
		return _availableBuffs.get(skillId);
	}

	public BuffSkillHolder getAvailableBuff(String groupType, int skillId)
	{
		Map<Integer, BuffSkillHolder> holdersByType = _availableBuffsByType.get(groupType);
		return holdersByType != null ? holdersByType.getOrDefault(skillId, _availableBuffs.get(skillId)) : _availableBuffs.get(skillId);
	}

	public static SchemeBufferTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private void loadAvailableBuffs()
	{
		int categoryCount = 0;
		int skillCount = 0;

		try
		{
			Node rootNode = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("./data/SchemeBufferSkills.xml")).getDocumentElement();

			for (Node categoryNode = rootNode != null ? rootNode.getFirstChild() : null; categoryNode != null; categoryNode = categoryNode.getNextSibling())
			{
				if ("category".equalsIgnoreCase(categoryNode.getNodeName()))
				{
					NamedNodeMap categoryAttributes = categoryNode.getAttributes();
					if (categoryAttributes != null)
					{
						Node typeNode = categoryAttributes.getNamedItem("type");
						if (typeNode != null)
						{
							String category = typeNode.getNodeValue();
							if (!_skillIdsByType.containsKey(category))
							{
								_skillIdsByType.put(category, new ArrayList<>());
								_availableBuffsByType.put(category, new LinkedHashMap<>());
								_skillTypesOrder.add(category);
								++categoryCount;
							}

							for (Node buffNode = categoryNode.getFirstChild(); buffNode != null; buffNode = buffNode.getNextSibling())
							{
								if ("buff".equalsIgnoreCase(buffNode.getNodeName()))
								{
									NamedNodeMap attributes = buffNode.getAttributes();
									if (attributes != null)
									{
										Node idNode = attributes.getNamedItem("id");
										Node levelNode = attributes.getNamedItem("level");
										Node priceNode = attributes.getNamedItem("price");
										if (idNode != null && levelNode != null && priceNode != null)
										{
											int skillId = Integer.parseInt(idNode.getNodeValue());
											int level = Integer.parseInt(levelNode.getNodeValue());
											int price = Integer.parseInt(priceNode.getNodeValue());
											Node descNode = attributes.getNamedItem("desc");
											String description = descNode != null ? descNode.getNodeValue() : "";
											List<Integer> skillIds = _skillIdsByType.get(category);
											if (!skillIds.contains(skillId))
											{
												skillIds.add(skillId);
											}

											BuffSkillHolder holder = new BuffSkillHolder(skillId, level, price, category, description);
											_availableBuffsByType.get(category).put(skillId, holder);
											BuffSkillHolder existing = _availableBuffs.get(skillId);
											if (existing == null || holder.getLevel() > existing.getLevel())
											{
												_availableBuffs.put(skillId, holder);
											}

											++skillCount;
										}
									}
								}
							}
						}
					}
				}
			}

			for (Map.Entry<String, List<Integer>> entry : _skillIdsByType.entrySet())
			{
				entry.setValue(Collections.unmodifiableList(entry.getValue()));
			}

			LOGGER.info("SchemeBufferTable: Loaded " + categoryCount + " categories and " + skillCount + " entries from " + SKILLS_XML_PATH);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "SchemeBufferTable: Failed to load skills XML: " + SKILLS_XML_PATH, e);
		}
	}

	private void loadPlayerSchemes()
	{
		int entryCount = 0;

		try
		{
			Connection connection = DatabaseFactory.getConnection();

			try
			{
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM buffer_schemes");

				try
				{
					ResultSet resultSet = statement.executeQuery();

					try
					{
						while (resultSet.next())
						{
							int objectId = resultSet.getInt("object_id");
							String schemeName = resultSet.getString("scheme_name");
							String skills = resultSet.getString("skills");
							if (schemeName != null && skills != null)
							{
								String[] split = skills.split(",");
								List<Integer> schemeSkillIds = new ArrayList<>(split.length);

								for (String token : split)
								{
									if (!token.isEmpty())
									{
										int skillId;
										try
										{
											skillId = Integer.parseInt(token);
										}
										catch (NumberFormatException ex)
										{
											LOGGER.warning("SchemeBufferTable: Invalid skill id '" + token + "' for scheme '" + schemeName + "' (objectId: " + objectId + ").");
											continue;
										}

										if (_availableBuffs.containsKey(skillId))
										{
											schemeSkillIds.add(skillId);
										}
									}
								}

								setScheme(objectId, schemeName, schemeSkillIds);
								++entryCount;
							}
						}

						LOGGER.info("SchemeBufferTable: Loaded " + entryCount + " scheme entries from database.");
					}
					catch (Throwable var20)
					{
						if (resultSet != null)
						{
							try
							{
								resultSet.close();
							}
							catch (Throwable var18)
							{
								var20.addSuppressed(var18);
							}
						}

						throw var20;
					}

					if (resultSet != null)
					{
						resultSet.close();
					}
				}
				catch (Throwable var21)
				{
					if (statement != null)
					{
						try
						{
							statement.close();
						}
						catch (Throwable var17)
						{
							var21.addSuppressed(var17);
						}
					}

					throw var21;
				}

				if (statement != null)
				{
					statement.close();
				}
			}
			catch (Throwable var22)
			{
				if (connection != null)
				{
					try
					{
						connection.close();
					}
					catch (Throwable var16)
					{
						var22.addSuppressed(var16);
					}
				}

				throw var22;
			}

			if (connection != null)
			{
				connection.close();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "SchemeBufferTable: Failed to load buff schemes from database.", e);
		}
	}

	private static class SingletonHolder
	{
		protected static final SchemeBufferTable INSTANCE = new SchemeBufferTable();
	}
}
