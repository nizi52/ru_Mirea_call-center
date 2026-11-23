-- Read-only normalization: no existing client or appeal records are modified.
CREATE FUNCTION normalize_client_phone(value TEXT) RETURNS TEXT
LANGUAGE SQL IMMUTABLE STRICT AS $$
 SELECT '+' || CASE WHEN length(d)=11 AND left(d,1)='8'
                    THEN '7'||substring(d FROM 2) ELSE d END
 FROM (SELECT regexp_replace(value,'[^0-9]','','g') AS d) digits;
$$;
CREATE INDEX clients_phone_lookup_idx ON clients(normalize_client_phone(phone));
