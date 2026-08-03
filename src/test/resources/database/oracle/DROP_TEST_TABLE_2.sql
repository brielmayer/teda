-- Oracle only knows "DROP TABLE IF EXISTS" from 23c on, so the drop is wrapped
-- in a block that swallows ORA-00942 (table or view does not exist).
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE STUDENT_2';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
