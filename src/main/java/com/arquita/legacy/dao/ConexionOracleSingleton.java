package com.arquita.legacy.dao;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConexionOracleSingleton {

    private static ConexionOracleSingleton instancia;

    private Connection conexion;
    private final String driverClassName;
    private final String url;
    private final String usuario;
    private final String clave;

    private ConexionOracleSingleton() {
        Properties props = leerPropiedadesDeConexion();
        this.driverClassName = props.getProperty("jdbc.driverClassName", "oracle.jdbc.driver.OracleDriver");
        this.url = props.getProperty("jdbc.url", "jdbc:oracle:thin:@localhost:1521:XE");
        this.usuario = props.getProperty("jdbc.username", "arquita_app");
        this.clave = props.getProperty("jdbc.password", "arquita_app_2013");
        try {
            Class.forName(driverClassName);
            this.conexion = DriverManager.getConnection(url, usuario, clave);
        } catch (Exception e) {
            System.out.println("Error al conectar via ConexionOracleSingleton: " + e.getMessage());
        }
    }

    private static Properties leerPropiedadesDeConexion() {
        Properties props = new Properties();
        InputStream in = null;
        try {
            in = ConexionOracleSingleton.class.getClassLoader().getResourceAsStream("jdbc.properties");
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            System.out.println("No se pudo leer jdbc.properties, se usan valores por defecto: " + e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
        }
        return props;
    }

    public static ConexionOracleSingleton getInstancia() {
        if (instancia == null) {
            instancia = new ConexionOracleSingleton();
        }
        return instancia;
    }

    public Connection getConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            conexion = DriverManager.getConnection(url, usuario, clave);
        }
        return conexion;
    }
}
