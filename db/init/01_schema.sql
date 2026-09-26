-- Se ejecuta como SYS conectado al CDB raiz (XE) al iniciar el contenedor
-- por primera vez (ver container-entrypoint-initdb.d en la doc de
-- gvenzl/oracle-xe). El usuario de la app vive en el pluggable database
-- XEPDB1, asi que hay que cambiar de contenedor antes de crear nada ahi.
ALTER SESSION SET CONTAINER = XEPDB1;
ALTER SESSION SET CURRENT_SCHEMA = ARQUITA_APP;

-- Mismo nombre que usa Hibernate 4 por defecto para @GeneratedValue(strategy = SEQUENCE)
-- sin @SequenceGenerator explicito: la comparten todas las entidades.
CREATE SEQUENCE HIBERNATE_SEQUENCE START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE SEQUENCE SEQ_NUMERO_COMPROBANTE START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE CONTRIBUYENTE (
    ID                NUMBER PRIMARY KEY,
    CUIT              VARCHAR2(13) NOT NULL,
    RAZON_SOCIAL      VARCHAR2(255),
    CONDICION_IVA     VARCHAR2(50),
    DOMICILIO_FISCAL  VARCHAR2(255),
    ACTIVO            NUMBER(1) DEFAULT 1
);

CREATE TABLE FACTURA (
    ID                    NUMBER PRIMARY KEY,
    NUMERO                NUMBER,
    PUNTO_VENTA           NUMBER,
    TIPO_COMPROBANTE      NUMBER NOT NULL,
    FECHA                 TIMESTAMP,
    CUIT_EMISOR           VARCHAR2(13),
    CUIT_RECEPTOR         VARCHAR2(13),
    IMPORTE_NETO          NUMBER,
    IMPORTE_IVA           NUMBER,
    IMPORTE_TOTAL         NUMBER,
    MONEDA                VARCHAR2(10),
    COTIZACION_AL_EMITIR  NUMBER,
    ESTADO                NUMBER NOT NULL,
    CAE                   VARCHAR2(50),
    CAE_VENCIMIENTO       TIMESTAMP
);

CREATE TABLE DETALLE_FACTURA (
    ID               NUMBER PRIMARY KEY,
    DESCRIPCION      VARCHAR2(255),
    CANTIDAD         NUMBER,
    PRECIO_UNITARIO  NUMBER,
    ALICUOTA_IVA     NUMBER,
    FACTURA_ID       NUMBER,
    CONSTRAINT FK_DETALLE_FACTURA FOREIGN KEY (FACTURA_ID) REFERENCES FACTURA(ID)
);

CREATE TABLE PAGO (
    ID          NUMBER PRIMARY KEY,
    FACTURA_ID  NUMBER NOT NULL,
    FECHA       TIMESTAMP,
    MONTO       NUMBER,
    MEDIO_PAGO  VARCHAR2(50),
    ESTADO      VARCHAR2(20),
    CONSTRAINT FK_PAGO_FACTURA FOREIGN KEY (FACTURA_ID) REFERENCES FACTURA(ID)
);

CREATE TABLE NOTA_CREDITO (
    ID                   NUMBER PRIMARY KEY,
    FACTURA_ORIGINAL_ID  NUMBER NOT NULL,
    NUMERO               NUMBER,
    PUNTO_VENTA          NUMBER,
    FECHA                TIMESTAMP,
    MOTIVO               VARCHAR2(255),
    IMPORTE              NUMBER,
    CAE                  VARCHAR2(50),
    CAE_VENCIMIENTO      TIMESTAMP,
    CONSTRAINT FK_NOTA_CREDITO_FACTURA FOREIGN KEY (FACTURA_ORIGINAL_ID) REFERENCES FACTURA(ID)
);
