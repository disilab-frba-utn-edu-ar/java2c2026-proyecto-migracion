ALTER SESSION SET CONTAINER = XEPDB1;
ALTER SESSION SET CURRENT_SCHEMA = ARQUITA_APP;

CREATE OR REPLACE PACKAGE PKG_FACTURACION AS

    PROCEDURE registrar_pago(
        p_factura_id  IN  FACTURA.ID%TYPE,
        p_monto       IN  NUMBER,
        p_medio_pago  IN  VARCHAR2,
        p_pago_id     OUT PAGO.ID%TYPE
    );

    PROCEDURE anular_factura(
        p_factura_id       IN  FACTURA.ID%TYPE,
        p_nota_credito_id  OUT NOTA_CREDITO.ID%TYPE
    );

END PKG_FACTURACION;
/

CREATE OR REPLACE PACKAGE BODY PKG_FACTURACION AS

    PROCEDURE registrar_pago(
        p_factura_id  IN  FACTURA.ID%TYPE,
        p_monto       IN  NUMBER,
        p_medio_pago  IN  VARCHAR2,
        p_pago_id     OUT PAGO.ID%TYPE
    ) IS
        v_estado         FACTURA.ESTADO%TYPE;
        v_importe_total  FACTURA.IMPORTE_TOTAL%TYPE;
        v_acumulado      NUMBER;
    BEGIN
        BEGIN
            SELECT ESTADO, IMPORTE_TOTAL INTO v_estado, v_importe_total
            FROM FACTURA WHERE ID = p_factura_id FOR UPDATE;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RAISE_APPLICATION_ERROR(-20010, 'Factura inexistente: ' || p_factura_id);
        END;

        IF v_estado = 0 THEN
            RAISE_APPLICATION_ERROR(-20011, 'No se puede pagar una factura en borrador.');
        ELSIF v_estado = 3 THEN
            RAISE_APPLICATION_ERROR(-20012, 'No se puede pagar una factura anulada.');
        END IF;

        SELECT HIBERNATE_SEQUENCE.NEXTVAL INTO p_pago_id FROM DUAL;

        INSERT INTO PAGO (ID, FACTURA_ID, FECHA, MONTO, MEDIO_PAGO, ESTADO)
        VALUES (p_pago_id, p_factura_id, SYSDATE, p_monto, p_medio_pago, 'CONFIRMADO');

        SELECT NVL(SUM(MONTO), 0) INTO v_acumulado FROM PAGO WHERE FACTURA_ID = p_factura_id;

        IF v_acumulado >= v_importe_total THEN
            UPDATE FACTURA SET ESTADO = 2 WHERE ID = p_factura_id;
        END IF;
    END registrar_pago;

    PROCEDURE anular_factura(
        p_factura_id       IN  FACTURA.ID%TYPE,
        p_nota_credito_id  OUT NOTA_CREDITO.ID%TYPE
    ) IS
        v_estado       FACTURA.ESTADO%TYPE;
        v_punto_venta  FACTURA.PUNTO_VENTA%TYPE;
        v_importe      FACTURA.IMPORTE_TOTAL%TYPE;
        v_cuit_emisor  FACTURA.CUIT_EMISOR%TYPE;
        v_numero       NOTA_CREDITO.NUMERO%TYPE;
        v_cae          NOTA_CREDITO.CAE%TYPE;
    BEGIN
        BEGIN
            SELECT ESTADO, PUNTO_VENTA, IMPORTE_TOTAL, CUIT_EMISOR
            INTO v_estado, v_punto_venta, v_importe, v_cuit_emisor
            FROM FACTURA WHERE ID = p_factura_id FOR UPDATE;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RAISE_APPLICATION_ERROR(-20030, 'Factura inexistente: ' || p_factura_id);
        END;

        p_nota_credito_id := NULL;
        IF v_estado = 2 THEN
            SELECT HIBERNATE_SEQUENCE.NEXTVAL INTO p_nota_credito_id FROM DUAL;
            SELECT SEQ_NUMERO_COMPROBANTE.NEXTVAL INTO v_numero FROM DUAL;
            v_cae := 'CAE' || REPLACE(v_cuit_emisor, '-', '') || TO_CHAR(SYSTIMESTAMP, 'FF6');

            INSERT INTO NOTA_CREDITO (
                ID, FACTURA_ORIGINAL_ID, NUMERO, PUNTO_VENTA, FECHA, MOTIVO, IMPORTE, CAE, CAE_VENCIMIENTO
            ) VALUES (
                p_nota_credito_id, p_factura_id, v_numero, v_punto_venta, SYSDATE,
                'Anulacion de factura pagada', v_importe, v_cae, SYSDATE + 10
            );
        END IF;

        UPDATE FACTURA SET ESTADO = 3 WHERE ID = p_factura_id;
    END anular_factura;

END PKG_FACTURACION;
/
