-- 1. Consultar todas las compras y ventas registradas
SELECT id,
       client_id,
       user_id,
       vehicle_id,
       contract_type,
       contract_status,
       payment_method,
       purchase_price,
       sale_price,
       created_at,
       updated_at
FROM purchase_sales
ORDER BY created_at DESC;

-- 2. Filtrar por tipo de contrato (Compra o Venta)
SELECT id,
       client_id,
       user_id,
       vehicle_id,
       contract_type,
       contract_status,
       sale_price,
       purchase_price
FROM purchase_sales
WHERE contract_type = 'SALE'
ORDER BY updated_at DESC;

-- 3. Mostrar solo contratos pendientes
SELECT id,
       client_id,
       user_id,
       vehicle_id,
       contract_type,
       payment_method,
       contract_status,
       sale_price,
       purchase_price
FROM purchase_sales
WHERE contract_status = 'PENDING'
ORDER BY created_at;

-- 4. Consultar el historial de transacciones por cliente
SELECT ps.id,
       ps.contract_type,
       ps.contract_status,
       ps.payment_method,
       ps.purchase_price,
       ps.sale_price,
       ps.created_at,
       ps.updated_at
FROM purchase_sales ps
WHERE ps.client_id = 1
ORDER BY ps.created_at DESC;

-- 5. Total de ingresos por ventas completadas
SELECT SUM(sale_price) AS total_ventas,
       COUNT(*)        AS cantidad_ventas
FROM purchase_sales
WHERE contract_type = 'SALE'
  AND contract_status = 'COMPLETED';

-- 6. Total invertido en compras completadas
SELECT SUM(purchase_price) AS total_compras,
       COUNT(*)            AS cantidad_compras
FROM purchase_sales
WHERE contract_type = 'PURCHASE'
  AND contract_status = 'COMPLETED';

-- 7. Utilidad bruta estimada por transacción
SELECT id,
       contract_type,
       purchase_price,
       sale_price,
       (sale_price - purchase_price) AS utilidad_bruta
FROM purchase_sales
WHERE contract_type = 'SALE'
  AND contract_status = 'COMPLETED'
ORDER BY utilidad_bruta DESC;

-- 8. Distribución de métodos de pago usados
SELECT payment_method,
       COUNT(*) AS cantidad_transacciones
FROM purchase_sales
GROUP BY payment_method
ORDER BY cantidad_transacciones DESC;

-- 9. Consultar últimas 5 transacciones actualizadas
SELECT id,
       contract_type,
       contract_status,
       payment_method,
       sale_price,
       purchase_price,
       updated_at
FROM purchase_sales
ORDER BY updated_at DESC
LIMIT 5;

-- 10. Agrupar ventas por estado del contrato
SELECT contract_status,
       COUNT(*)                  AS cantidad,
       ROUND(AVG(sale_price), 2) AS promedio_precio
FROM purchase_sales
WHERE contract_type = 'SALE'
GROUP BY contract_status
ORDER BY cantidad DESC;

-- 11. Identificar contratos sin observaciones
SELECT id,
       contract_type,
       contract_status,
       client_id,
       user_id,
       payment_method
FROM purchase_sales
WHERE observations IS NULL
   OR TRIM(observations) = '';

-- 12. Transacciones entre dos fechas específicas
SELECT id,
       contract_type,
       client_id,
       sale_price,
       purchase_price,
       created_at
FROM purchase_sales
WHERE created_at BETWEEN '2025-01-01' AND '2025-12-31'
ORDER BY created_at DESC;

-- 13. Diferencia entre precio de venta y compra promedio
SELECT ROUND(AVG(sale_price - purchase_price), 2) AS margen_promedio
FROM purchase_sales
WHERE contract_status = 'COMPLETED';

-- 14. Contratos modificados recientemente (últimos 7 días)
SELECT id,
       contract_type,
       client_id,
       vehicle_id,
       updated_at
FROM purchase_sales
WHERE updated_at >= NOW() - INTERVAL '7 days'
ORDER BY updated_at DESC;

-- 15. Simulación de unión con la tabla de vehículos
-- (solo si existe el microservicio de inventario sgivu-vehicle)
-- SELECT
--     ps.id,
--     v.brand,
--     v.model,
--     v.plate,
--     ps.contract_type,
--     ps.contract_status,
--     ps.sale_price
-- FROM purchase_sales ps
-- JOIN vehicles v ON ps.vehicle_id = v.id
-- ORDER BY ps.created_at DESC;
