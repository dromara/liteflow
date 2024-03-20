DELETE
FROM EL_TABLE;

INSERT INTO EL_TABLE (APPLICATION_NAME, CHAIN_NAME, EL_DATA)
values ('demo', 'chain11', 'THEN(a, b, c);');
INSERT INTO EL_TABLE (APPLICATION_NAME, CHAIN_NAME, EL_DATA)
values ('demo', 'chain21', 'THEN(a, b, c);');
INSERT INTO EL_TABLE (APPLICATION_NAME, CHAIN_NAME, EL_DATA)
values ('demo', 'chain31', 'IF(x0, THEN(a, b));');
INSERT INTO EL_TABLE (APPLICATION_NAME, CHAIN_NAME, EL_DATA)
values ('demo', '<chain31>', 'IF(x0, THEN(a, b));');
INSERT INTO EL_TABLE (APPLICATION_NAME, CHAIN_NAME, EL_DATA)
values ('demo', 'chain41', 'IF(x2, IF(x0, THEN(a, b)));');

DELETE
FROM SCRIPT_NODE_TABLE;

INSERT INTO SCRIPT_NODE_TABLE (APPLICATION_NAME, SCRIPT_NODE_ID, SCRIPT_NODE_NAME, SCRIPT_NODE_TYPE, SCRIPT_NODE_DATA,
                               SCRIPT_LANGUAGE)
values ('demo', 'x01', 'if 脚本', 'boolean_script', 'return true', 'groovy');
INSERT INTO SCRIPT_NODE_TABLE (APPLICATION_NAME, SCRIPT_NODE_ID, SCRIPT_NODE_NAME, SCRIPT_NODE_TYPE, SCRIPT_NODE_DATA,
                               SCRIPT_LANGUAGE)
values ('demo', 'x11', 'if 脚本', 'boolean_script', 'return false', 'groovy');

INSERT INTO SCRIPT_NODE_TABLE (APPLICATION_NAME, SCRIPT_NODE_ID, SCRIPT_NODE_NAME, SCRIPT_NODE_TYPE, SCRIPT_NODE_DATA,
                               SCRIPT_LANGUAGE)
values ('demo', 'x21', 'python脚本', 'boolean_script', 'return true', 'js');
