package de.htwk.autolat.Configuration;

import java.util.Date;
import java.util.List;

import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.persistence.DBQuery;

import de.htwk.autolat.ServerConnection.ServerConnection;
import de.htwk.autolat.TaskConfiguration.TaskConfiguration;
import de.htwk.autolat.TaskInstance.TaskInstance;
import de.htwk.autolat.TaskModule.TaskModule;

public class ConfigurationManagerImpl implements ConfigurationManager {
	
	public static final ConfigurationManagerImpl INSTANCE = new ConfigurationManagerImpl();
	
	public ConfigurationManagerImpl() {
		//nothing to do here
	}

	@Override
	public Configuration createConfiguration(TaskConfiguration taskConfiguration, List<TaskModule> taskPlan,
			List<TaskInstance> taskInstanceList, Date beginDate, long courseID, long courseNodeID, Date endDate, List<Integer> scorePoints,
			ServerConnection serverConnection, String autolatServer) {
		Configuration conf = new ConfigurationImpl();
		conf.setBeginDate(beginDate);
		conf.setCourseID(courseID);
		conf.setCourseNodeID(courseNodeID);
		conf.setEndDate(endDate);
		conf.setScorePoints(scorePoints);
		conf.setServerConnection(serverConnection);
		conf.setTaskConfiguration(taskConfiguration);
		conf.setTaskInstanceList(taskInstanceList);
		conf.setTaskPlan(taskPlan);
		conf.setAutolatServer(autolatServer);
		return conf;
	}
	
	@Override
	public Configuration createAndPersistConfiguration (TaskConfiguration taskConfiguration, List<TaskModule> taskPlan,
			List<TaskInstance> taskInstanceList, Date beginDate, long courseID, long courseNodeID, Date endDate, List<Integer> scorePoints,
			ServerConnection serverConnection, String autolatServer) {
	
		Configuration conf = createConfiguration(taskConfiguration,taskPlan, taskInstanceList, beginDate, courseID, courseNodeID, 
				endDate, scorePoints, serverConnection, autolatServer);
		saveConfiguration(conf);
		return conf;
	}

	@Override
	public Configuration createAndPersistConfiguration(long courseID, long courseNodeID) {
		return createAndPersistConfiguration(null, null, null, null, courseID, courseNodeID, null, null, null, null);
	}

	@Override
	public boolean deleteConfiguration(Configuration conf) {
		try {
			DBFactory.getInstance().deleteObject(conf);
			return true;
		}catch(Exception e) {
			return false;
		}
	}

	@Override
	public Configuration findConfigurationByCourseID(long courseID, long courseNodeID) {
		String query = "SELECT conf FROM ConfigurationImpl AS conf WHERE conf.courseNodeID = :cnid AND conf.courseID = :cid";
		DBQuery dbq = DBFactory.getInstance().createQuery(query);
		dbq.setLong("cid", courseID);
		dbq.setLong("cnid", courseNodeID);

		@SuppressWarnings("rawtypes")
		List result = dbq.list();
		if(result.size() == 0) {
			return null;
		} else {
			return (Configuration) result.get(0);
		}
	}

	@Override
	public Configuration getConfigurationByCourseID(long courseID, long courseNodeID) {
		Configuration conf = findConfigurationByCourseID(courseID, courseNodeID);

		if(conf == null) {
			conf = createAndPersistConfiguration(courseID, courseNodeID);
		}

		return conf;
	}
	
	@Override
	public Configuration getConfigurationByTaskConfiguration(TaskConfiguration taskConfig) {
		
		String query = "SELECT conf FROM ConfigurationImpl AS conf WHERE conf.taskConfiguration = :key";
		DBQuery dbq = DBFactory.getInstance().createQuery(query);
		dbq.setLong("key", taskConfig.getKey()); 
		List result = dbq.list();
		return (result.size() > 0 ? (Configuration) result.get(0) : null);
	}

	@Override
	public Configuration loadConfigurationByID(long ID) {
		try {
			return DBFactory.getInstance().getCurrentEntityManager().find(ConfigurationImpl.class, ID);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void saveConfiguration(Configuration conf) {
		DBFactory.getInstance().saveObject(conf);
	}

	@Override
	public void updateConfiguration(Configuration conf) {
		DBFactory.getInstance().updateObject(conf);
	}
	
	public static ConfigurationManagerImpl getInstance() {
		return INSTANCE;
	}
}
