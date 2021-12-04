/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package win.liyufan.im;


import java.beans.PropertyVetoException;
import java.sql.*;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import io.moquette.BrokerConstants;
import io.moquette.server.config.IConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class DBUtil {
    private static final Logger LOG = LoggerFactory.getLogger(DBUtil.class);
    private static ComboPooledDataSource comboPooledDataSource = null;
    private static ConcurrentHashMap<Long, String>map = new ConcurrentHashMap<>();
    private static ThreadLocal<Connection> transactionConnection = new ThreadLocal<Connection>() {
        @Override
        protected Connection initialValue() {
            super.initialValue();
            return null;
        }
    };
    public static SqlStyle DBSTYLE = SqlStyle.MySql_Style;

    //
    public enum SqlStyle {
        MySql_Style, //support on duplicate key update
        PgSql_Style, //support on conflict (key) do update
        Oracle_Style,  //support merge into
        MSSQL_Style
    }

    public static void init(IConfig config) {
        String embedDB = config.getProperty(BrokerConstants.EMBED_DB_PROPERTY_NAME);
        if (embedDB != null && embedDB.equals("0")) {
            LOG.info("Use mysql database");
            DBSTYLE = SqlStyle.MySql_Style;
            comboPooledDataSource = new ComboPooledDataSource("mysql");
        } else if(embedDB != null && embedDB.equals("2")) {
            LOG.info("Use mysql database");
            DBSTYLE = SqlStyle.MySql_Style;
            comboPooledDataSource = new ComboPooledDataSource("mysql");
        } else if (embedDB != null && embedDB.equals("3")) {
            LOG.info("Use kingbase database");
            DBSTYLE = SqlStyle.PgSql_Style;
            comboPooledDataSource = new ComboPooledDataSource("kingbase-v8");
        } else if (embedDB != null && embedDB.equals("4")) {
            LOG.info("Use dameng database");
            DBSTYLE = SqlStyle.Oracle_Style;
            comboPooledDataSource = new ComboPooledDataSource("dameng8");
        } else if(embedDB != null && embedDB.equals("5")) {
            LOG.info("Use mssql database");
            DBSTYLE = SqlStyle.MSSQL_Style;
            comboPooledDataSource = new ComboPooledDataSource("mssql");
        } else if(embedDB != null && embedDB.equals("6")) {
            LOG.info("Use postgresql database");
            DBSTYLE = SqlStyle.PgSql_Style;
            comboPooledDataSource = new ComboPooledDataSource("pgsql");
        } else {
            LOG.info("Invalid db config. Can not user h2db");
            System.out.println("Invalid db config. Can not user h2db");
            System.exit(-1);
        }
    }

    //从数据源中获取数据库的连接
    public static Connection getConnection() throws SQLException {
        long threadId = Thread.currentThread().getId();

        if (map.get(threadId) != null) {
            LOG.error("error here!!!! DB connection not close correctly");
        }
        map.put(threadId, Thread.currentThread().getStackTrace().toString());
        Connection connection = transactionConnection.get();
        if (connection != null) {
            LOG.debug("Thread {} get db connection {}", threadId, connection);
            return connection;
        }

        connection = comboPooledDataSource.getConnection();
        LOG.debug("Thread {} get db connection {}", threadId, connection);
        return connection;
    }

    //释放资源，将数据库连接还给数据库连接池
    public static void closeDB(Connection conn,PreparedStatement ps,ResultSet rs) {
        LOG.debug("Thread {} release db connection {}", Thread.currentThread().getId(), conn);
        try {
            if (rs!=null) {
                rs.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (ps!=null) {
                ps.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (conn!=null && transactionConnection.get() != conn) {
                conn.close();
                map.remove(Thread.currentThread().getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getUserSecret(String userId, String clientId) {
        String sql;
        if(DBUtil.DBSTYLE == DBUtil.SqlStyle.MSSQL_Style) {
            sql = "select top 1 _secret from t_user_session where _uid = ? and _cid = ?";
        } else {
            sql = "select _secret from t_user_session where _uid = ? and _cid = ? limit 1";
        }
        Connection connection = null;
        PreparedStatement statement = null;

        ResultSet resultSet = null;
        try {
            connection = DBUtil.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, clientId);

            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String secret = resultSet.getString(1);
                return secret;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtil.closeDB(connection, statement, resultSet);
        }
        return null;
    }
}
