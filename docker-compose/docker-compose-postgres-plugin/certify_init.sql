CREATE DATABASE inji_certify
  ENCODING = 'UTF8'
  LC_COLLATE = 'en_US.UTF-8'
  LC_CTYPE = 'en_US.UTF-8'
  TABLESPACE = pg_default
  OWNER = postgres
  TEMPLATE  = template0;

COMMENT ON DATABASE inji_certify IS 'certify related data is stored in this database';

\c inji_certify postgres

DROP SCHEMA IF EXISTS certify CASCADE;
CREATE SCHEMA certify;
ALTER SCHEMA certify OWNER TO postgres;
ALTER DATABASE inji_certify SET search_path TO certify,pg_catalog,public;

CREATE TABLE certify.key_alias(
                                  id character varying(36) NOT NULL,
                                  app_id character varying(36) NOT NULL,
                                  ref_id character varying(128),
                                  key_gen_dtimes timestamp,
                                  key_expire_dtimes timestamp,
                                  status_code character varying(36),
                                  lang_code character varying(3),
                                  cr_by character varying(256) NOT NULL,
                                  cr_dtimes timestamp NOT NULL,
                                  upd_by character varying(256),
                                  upd_dtimes timestamp,
                                  is_deleted boolean DEFAULT FALSE,
                                  del_dtimes timestamp,
                                  cert_thumbprint character varying(100),
                                  uni_ident character varying(50),
                                  CONSTRAINT pk_keymals_id PRIMARY KEY (id),
                                  CONSTRAINT uni_ident_const UNIQUE (uni_ident)
);

CREATE TABLE certify.key_policy_def(
                                       app_id character varying(36) NOT NULL,
                                       key_validity_duration smallint,
                                       is_active boolean NOT NULL,
                                       pre_expire_days smallint,
                                       access_allowed character varying(1024),
                                       cr_by character varying(256) NOT NULL,
                                       cr_dtimes timestamp NOT NULL,
                                       upd_by character varying(256),
                                       upd_dtimes timestamp,
                                       is_deleted boolean DEFAULT FALSE,
                                       del_dtimes timestamp,
                                       CONSTRAINT pk_keypdef_id PRIMARY KEY (app_id)
);

CREATE TABLE certify.key_store(
                                  id character varying(36) NOT NULL,
                                  master_key character varying(36) NOT NULL,
                                  private_key character varying(2500) NOT NULL,
                                  certificate_data character varying NOT NULL,
                                  cr_by character varying(256) NOT NULL,
                                  cr_dtimes timestamp NOT NULL,
                                  upd_by character varying(256),
                                  upd_dtimes timestamp,
                                  is_deleted boolean DEFAULT FALSE,
                                  del_dtimes timestamp,
                                  CONSTRAINT pk_keystr_id PRIMARY KEY (id)
);

CREATE TABLE certify.svg_template (
                                    id UUID NOT NULL,
                                    template VARCHAR NOT NULL,
                                    cr_dtimes timestamp NOT NULL,
                                    upd_dtimes timestamp,
                                    CONSTRAINT pk_svgtmp_id PRIMARY KEY (id)
);

CREATE TABLE certify.template_data(
                                    context character varying(1024) NOT NULL,
                                    credential_type character varying(512) NOT NULL,
                                    template VARCHAR NOT NULL,
                                    cr_dtimes timestamp NOT NULL default now(),
                                    upd_dtimes timestamp,
                                    CONSTRAINT pk_template PRIMARY KEY (context, credential_type)
);

CREATE TABLE certify.registration_receipt_data (
    registration_id VARCHAR(36) NOT NULL,
    car_registration_number VARCHAR(255),
    registration_date VARCHAR,
    rural_property_name VARCHAR(255),
    municipality VARCHAR(255),
    latitude VARCHAR(50),
    longitude VARCHAR(50),
    total_area NUMERIC(12,4),
    fiscal_modules NUMERIC(12,4),
    protocol_code VARCHAR(255),
    cpf VARCHAR(20),
    holder_name VARCHAR(255),
    total_area_declared NUMERIC(12,4),
    administrative_easement_area NUMERIC(12,4),
    net_area NUMERIC(12,4),
    consolidated_area NUMERIC(12,4),
    native_vegetation_remnant NUMERIC(12,4),
    legal_reserve_area NUMERIC(12,4),
    permanent_preservation_area NUMERIC(12,4),
    restricted_use_area NUMERIC(12,4),
	CONSTRAINT pk_reg_id_code PRIMARY KEY (registration_id)
);


CREATE TABLE certify.statement_data(
    statement_id VARCHAR(36) NOT NULL,
    car_registration_number VARCHAR(255),
    registration_date VARCHAR,
    date_of_last_amendment VARCHAR,
    rural_property_area NUMERIC(12,4),
    latitude VARCHAR(50),
    longitude VARCHAR(50),
    municipality VARCHAR(255),
    external_condition VARCHAR(255),
    registration_status VARCHAR(100),
    pra_condition VARCHAR(255),
    land_cover VARCHAR(255),
    native_vegetation_remnant_area NUMERIC(12,4),
    consolidated_rural_area NUMERIC(12,4),
    administrative_easement_area NUMERIC(12,4),
    location_of_legal_reserve VARCHAR(255),
    registered_legal_reserve_area NUMERIC(12,4),
    georeferenced_legal_reserve_area NUMERIC(12,4),
    approved_legal_reserve_area_not_registered NUMERIC(12,4),
    proposed_legal_reserve_area NUMERIC(12,4),
    total_legal_reserve_declared NUMERIC(12,4),
    permanent_preservation_areas_app NUMERIC(12,4),
    app_in_consolidated_rural_area NUMERIC(12,4),
    app_in_native_vegetation_remnant_area NUMERIC(12,4),
    restricted_use_areas NUMERIC(12,4),
    legal_reserve_deficit_excess NUMERIC(12,4),
    legal_reserve_area_to_recompose NUMERIC(12,4),
    permanent_preservation_areas_to_recompose NUMERIC(12,4),
    restricted_use_area_to_recompose NUMERIC(12,4),
    embargoed_area_description VARCHAR(255),
    embargoed_area_processing_date VARCHAR,
    embargoed_area_overlap NUMERIC(12,4),
    embargoed_area_overlap_percentage NUMERIC(7,4),
    conservation_unit_description VARCHAR(255),
    conservation_unit_processing_date VARCHAR,
    conservation_unit_overlap_area NUMERIC(12,4),
    conservation_unit_overlap_percentage NUMERIC(7,4),
	CONSTRAINT pk_stmt_id_code PRIMARY KEY (statement_id)
);

INSERT INTO certify.template_data (context, credential_type, template, cr_dtimes, upd_dtimes) VALUES ('https://vharsh.github.io/DID/mock-context.json,https://www.w3.org/2018/credentials/v1', 'MockVerifiableCredential,VerifiableCredential', '{
    "@context": [
    "https://www.w3.org/2018/credentials/v1",
    "https://vharsh.github.io/DID/mock-context.json"],
    "issuer": "${issuer}",
    "type": ["VerifiableCredential", "MockVerifiableCredential"],
    "issuanceDate": "${validFrom}",
    "expirationDate": "${validUntil}",
    "credentialSubject": {
        "gender": ${gender},
        "postalCode": ${postalCode},
        "fullName": ${fullName},
        "dateOfBirth": "${dateOfBirth}",
        "province": ${province},
        "phone": "${phone}",
        "addressLine1": ${addressLine1},
        "region": ${region},
        "vcVer": "${vcVer}",
        "UIN": ${UIN},
        "email": "${email}",
        "face": "${face}"
    }
}', '2024-10-22 17:08:17.826851', NULL);
INSERT INTO certify.template_data (context, credential_type, template, cr_dtimes, upd_dtimes) VALUES ('https://vharsh.github.io/DID/mock-context.json,https://www.w3.org/ns/credentials/v2', 'MockVerifiableCredential,VerifiableCredential', '{
    "@context": [
            "https://www.w3.org/ns/credentials/v2", "https://vharsh.github.io/DID/mock-context.json"],
    "issuer": "${issuer}",
    "type": ["VerifiableCredential", "MockVerifiableCredential"],
    "validFrom": "${validFrom}",
    "validUntil": "${validUntil}",
    "credentialSubject": {
    "gender": ${gender},
        "postalCode": ${postalCode},
        "fullName": ${fullName},
        "dateOfBirth": "${dateOfBirth}",
        "province": ${province},
        "phone": "${phone}",
        "addressLine1": ${addressLine1},
        "region": ${region},
        "vcVer": "${vcVer}",
        "UIN": ${UIN},
        "email": "${email}",
        "face": "${face}"
    }
}', '2024-10-22 17:08:17.826851', NULL);
INSERT INTO certify.template_data (context, credential_type, template, cr_dtimes, upd_dtimes) VALUES ('https://www.w3.org/2018/credentials/v1', 'FarmerCredential,VerifiableCredential', '{
     "@context": [
                "https://www.w3.org/2018/credentials/v1",
                "https://vharsh.github.io/DID/farmer.json",
    "https://w3id.org/security/suites/ed25519-2020/v1"
        ],
        "issuer": "${issuer}",
        "type": [
            "VerifiableCredential",
            "FarmerCredential"
        ],
        "issuanceDate": "${validFrom}",
        "expirationDate": "${validUntil}",
        "credentialSubject": {
            "name": "${name}",
            "dateOfBirth": "${dateOfBirth}",
            "highestEducation": "${highestEducation}",
            "maritalStatus": "${maritalStatus}",
            "typeOfHouse": "${typeOfHouse}",
            "numberOfDependents": "${numberOfDependents}",
            "phoneNumber": "${phoneNumber}",
            "works": "${works}",
            "landArea": "${landArea}",
            "landOwnershipType": "${landOwnershipType}",
            "primaryCropType": "${primaryCropType}",
            "secondaryCropType": "${secondaryCropType}"
        }
}
', '2024-10-24 12:32:38.065994', NULL);

--Registration Receipt Template
INSERT INTO certify.template_data (context, credential_type, template, cr_dtimes, upd_dtimes)
VALUES (
    'https://piyush7034.github.io/my-files/registration-receipt.json,https://w3id.org/security/suites/ed25519-2020/v1,https://www.w3.org/2018/credentials/v1',
    'RegistrationReceiptCredential,VerifiableCredential',
    '{
         "@context": [
                 "https://www.w3.org/2018/credentials/v1",
                 "https://piyush7034.github.io/my-files/registration-receipt.json",
                 "https://w3id.org/security/suites/ed25519-2020/v1"
         ],
         "issuer": "${issuer}",
         "type": [
             "VerifiableCredential",
             "RegistrationReceiptCredential"
         ],
         "issuanceDate": "${validFrom}",
         "expirationDate": "${validUntil}",
         "credentialSubject": {
             "carRegistrationNumber": "${carRegistrationNumber}",
             "registrationDate": "${registrationDate}",
             "ruralPropertyName": "${ruralPropertyName}",
             "municipality": "${municipality}",
             "latitude": "${latitude}",
             "longitude": "${longitude}",
             "totalArea": ${totalArea},
             "fiscalModules": ${fiscalModules},
             "protocolCode": "${protocolCode}",
             "cpf": "${cpf}",
             "holderName": "${holderName}",
             "totalAreaDeclared": ${totalAreaDeclared},
             "administrativeEasementArea": ${administrativeEasementArea},
             "netArea":${netArea},
             "consolidatedArea": ${consolidatedArea},
             "nativeVegetationRemnant": ${nativeVegetationRemnant},
             "legalReserveArea": ${legalReserveArea},
             "permanentPreservationArea":${permanentPreservationArea},
             "restrictedUseArea": ${restrictedUseArea}
         }
     }
', '2024-10-24 12:32:38.065994', NULL);

-- Statement Template
INSERT INTO certify.template_data (context, credential_type, template, cr_dtimes, upd_dtimes)
VALUES (
    'https://piyush7034.github.io/my-files/statement.json,https://w3id.org/security/suites/ed25519-2020/v1,https://www.w3.org/2018/credentials/v1',
    'StatementCredential,VerifiableCredential',
    '{
         "@context": [
                 "https://www.w3.org/2018/credentials/v1",
                 "https://piyush7034.github.io/my-files/statement.json",
                 "https://w3id.org/security/suites/ed25519-2020/v1"
         ],
         "issuer": "${issuer}",
         "type": [
             "VerifiableCredential",
             "StatementCredential"
         ],
         "issuanceDate": "${validFrom}",
         "expirationDate": "${validUntil}",
         "credentialSubject": {
             "carRegistrationNumber": "${carRegistrationNumber}",
             "registrationDate": "${registrationDate}",
             "dateOfLastAmendment": "${dateOfLastAmendment}",
             "ruralPropertyArea": ${ruralPropertyArea},
             "latitude": "${latitude}",
             "longitude": "${longitude}",
             "municipality": "${municipality}",
             "externalCondition": "${externalCondition}",
             "registrationStatus": "${registrationStatus}",
             "praCondition": "${praCondition}",
             "landCover": "${landCover}",
             "nativeVegetationRemnantArea": ${nativeVegetationRemnantArea},
             "consolidatedRuralArea": ${consolidatedRuralArea},
             "administrativeEasementArea":${administrativeEasementArea},
             "locationOfLegalReserve": "${locationOfLegalReserve}",
             "registeredLegalReserveArea": ${registeredLegalReserveArea},
             "georeferencedLegalReserveArea": ${georeferencedLegalReserveArea},
             "approvedLegalReserveAreaNotRegistered": ${approvedLegalReserveAreaNotRegistered},
             "proposedLegalReserveArea": ${proposedLegalReserveArea},
             "totalLegalReserveDeclared": ${totalLegalReserveDeclared},
             "permanentPreservationAreasAPP": ${permanentPreservationAreasAPP},
             "appInConsolidatedRuralArea": ${appInConsolidatedRuralArea},
             "appInNativeVegetationRemnantArea":${appInNativeVegetationRemnantArea},
             "restrictedUseAreas": ${restrictedUseAreas},
             "legalReserveDeficitExcess": ${legalReserveDeficitExcess},
             "legalReserveAreaToRecompose": ${legalReserveAreaToRecompose},
             "permanentPreservationAreasToRecompose": ${permanentPreservationAreasToRecompose},
             "restrictedUseAreaToRecompose": ${restrictedUseAreaToRecompose},
             "embargoedAreaDescription": "${embargoedAreaDescription}",
             "embargoedAreaProcessingDate": "${embargoedAreaProcessingDate}",
             "embargoedAreaOverlap": ${embargoedAreaOverlap},
             "embargoedAreaOverlapPercentage":${embargoedAreaOverlapPercentage},
             "conservationUnitDescription": "${conservationUnitDescription}",
             "conservationUnitProcessingDate": "${conservationUnitProcessingDate}",
             "conservationUnitOverlapArea": ${conservationUnitOverlapArea},
             "conservationUnitOverlapPercentage": ${conservationUnitOverlapPercentage}
         }
     }
', '2024-10-24 12:32:38.065994', NULL);


INSERT INTO certify.key_policy_def(APP_ID,KEY_VALIDITY_DURATION,PRE_EXPIRE_DAYS,ACCESS_ALLOWED,IS_ACTIVE,CR_BY,CR_DTIMES) VALUES('ROOT', 2920, 1125, 'NA', true, 'mosipadmin', now());
INSERT INTO certify.key_policy_def(APP_ID,KEY_VALIDITY_DURATION,PRE_EXPIRE_DAYS,ACCESS_ALLOWED,IS_ACTIVE,CR_BY,CR_DTIMES) VALUES('CERTIFY_SERVICE', 1095, 60, 'NA', true, 'mosipadmin', now());
INSERT INTO certify.key_policy_def(APP_ID,KEY_VALIDITY_DURATION,PRE_EXPIRE_DAYS,ACCESS_ALLOWED,IS_ACTIVE,CR_BY,CR_DTIMES) VALUES('CERTIFY_PARTNER', 1095, 60, 'NA', true, 'mosipadmin', now());
INSERT INTO certify.key_policy_def(APP_ID,KEY_VALIDITY_DURATION,PRE_EXPIRE_DAYS,ACCESS_ALLOWED,IS_ACTIVE,CR_BY,CR_DTIMES) VALUES('CERTIFY_MOCK_RSA', 1095, 60, 'NA', true, 'mosipadmin', now());
INSERT INTO certify.key_policy_def(APP_ID,KEY_VALIDITY_DURATION,PRE_EXPIRE_DAYS,ACCESS_ALLOWED,IS_ACTIVE,CR_BY,CR_DTIMES) VALUES('CERTIFY_MOCK_ED25519', 1095, 60, 'NA', true, 'mosipadmin', now());
INSERT INTO certify.key_policy_def(APP_ID,KEY_VALIDITY_DURATION,PRE_EXPIRE_DAYS,ACCESS_ALLOWED,IS_ACTIVE,CR_BY,CR_DTIMES) VALUES('BASE', 1095, 60, 'NA', true, 'mosipadmin', now());

INSERT INTO certify.registration_receipt_data (
    registration_id,
    car_registration_number,
    registration_date,
    rural_property_name,
    municipality,
    latitude,
    longitude,
    total_area,
    fiscal_modules,
    protocol_code,
    cpf,
    holder_name,
    total_area_declared,
    administrative_easement_area,
    net_area,
    consolidated_area,
    native_vegetation_remnant,
    legal_reserve_area,
    permanent_preservation_area,
    restricted_use_area
)
VALUES
    ('1234567',
     'GO-5220009-75F5.A618.6575.4003.BC3B.3901.93D4.E5C0',
     '2024-10-25T16:40:31Z',
     'Fazenda teste',
     'São João d''Aliança, State: Goiás',
     '14°10''13.25\" S',
     '47°22''10.38\" W',
     1167.1764,
     16.6739,
     'GO-5220009-6C3F.E6F7.FB51.C627.CC38.D58A.2A48.FCC8',
     '931.877.970-40',
     'Teste nome',
     1167.1764,
     0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
    ('1234',
     'GO-5220010-85F6.B619.7576.5004.CD4C.4902.04D5.F6D1',
     '2024-10-26T14:30:45Z',
     'Fazenda Esperança',
     'Brasília, State: Distrito Federal',
     '15°11''14.36\" S',
     '48°23''11.49\" W',
     892.5632,
     12.5678,
     'GO-5220010-7D4E.F8G9.GC62.D738.DD49.E69B.3B59.GDD9',
     '654.321.987-00',
     'João Silva',
     892.5632,
     50.2134, 45.6789, 612.3456, 145.6782, 105.4321, 55.4321, 40.5678),
    ('4567538769',
     'GO-5220009-12A3.B456.7890.1234.DEF5.6789.0123.ABCD',
     '2024-10-25T16:40:31Z',
     'Sítio São João',
     'Formosa, Estado: Goiás',
     '15°45''23.12\" S',
     '47°18''45.32\" W',
     856.4321,
     12.3456,
     'GO-5220009-9876.5432.1098.7654.3210.9876.5432.1098',
     '123.456.789-10',
     'José da Silva Santos',
     856.4321,
     0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
    ('4567538771',
     'GO-5220010-98Z7.Y654.3210.9876.XWV5.4321.0987.EFGH',
     '2024-10-26T14:30:45Z',
     'Fazenda Bela Vista',
     'Planaltina, Estado: Goiás',
     '15°32''45.89\" S',
     '47°35''12.67\" W',
     945.7890,
     13.5678,
     'GO-5220010-5432.1098.7654.3210.9876.5432.1098.7654',
     '987.654.321-00',
     'Maria Aparecida Oliveira',
     945.7890,
     45.6789, 52.3456, 678.4321, 156.7890, 98.7654, 45.6789, 35.4321);


INSERT INTO certify.statement_data (
	statement_id,
    car_registration_number,
    registration_date,
    date_of_last_amendment,
    rural_property_area,
    latitude,
    longitude,
    municipality,
    external_condition,
    registration_status,
    pra_condition,
    land_cover,
    native_vegetation_remnant_area,
    consolidated_rural_area,
    administrative_easement_area,
    location_of_legal_reserve,
    registered_legal_reserve_area,
    georeferenced_legal_reserve_area,
    approved_legal_reserve_area_not_registered,
    proposed_legal_reserve_area,
    total_legal_reserve_declared,
    permanent_preservation_areas_app,
    app_in_consolidated_rural_area,
    app_in_native_vegetation_remnant_area,
    restricted_use_areas,
    legal_reserve_deficit_excess,
    legal_reserve_area_to_recompose,
    permanent_preservation_areas_to_recompose,
    restricted_use_area_to_recompose,
    embargoed_area_description,
    embargoed_area_processing_date,
    embargoed_area_overlap,
    embargoed_area_overlap_percentage,
    conservation_unit_description,
    conservation_unit_processing_date,
    conservation_unit_overlap_area,
    conservation_unit_overlap_percentage
)
VALUES
    ('1234567','GO-5220009-75F5.A618.6575.4003.BC3B.3901.93D4.E5C0',
     '2024-10-25 16:40:31',
     '2024-10-25',
     1167.1764,
     '14°10''13.25\" S',
     '47°22''10.38\" W',
     'João d''Aliança',
     'Awaiting analysis',
     'Active',
     '', '', 0.0, 0.0, 0.0,
     'Not analyzed Reserva Legal',
     0.0, 0.0, 0.0, 0.0, 0.0,
     0.0, 0.0, 0.0, 0.0,
     -233.4353, 0.0, 0.0, 0.0,
     'Infraction: Flora Infraction (Unclassified-Mobile)',
     '2024-10-25',
     748.1881, 64.1024,
     'Environmental Protection Area - POUSO ALTO ENVIRONMENTAL PROTECTION AREA',
     '2024-10-25',
     1167.1764, 100.0),
    ('1234','GO-5220010-85F6.B619.7576.5004.CD4C.4902.04D5.F6D1',
     '2024-10-26 14:30:45',
     '2024-10-26',
     892.5632,
     '15°11''14.36\" S',
     '48°23''11.49\" W',
     'Brasília',
     'Under Review',
     'Pending',
     'Preliminary', 'Mixed Vegetation',
     145.6782, 612.3456, 50.2134,
     'Partially Mapped',
     105.4321, 98.7654, 12.3456,
     95.6543, 90.1234,
     55.4321, 30.2109, 25.6789,
     40.5678, -156.7890,
     25.4321, 18.7654, 12.3456,
     'Potential Environmental Impact',
     '2024-10-26',
     456.7890, 51.2345,
     'Sustainable Development Reserve - MAMIRAUÁ',
     '2024-10-26',
     892.5632, 100.0),
    ('4567538772','GO-5220011-95F7.C620.8577.6005.DE5D.5903.15E6.G7E2',
     '2024-10-27 09:15:22',
     '2024-10-27',
     1546.2981,
     '16°12''15.47\" S',
     '49°24''12.60\" W',
     'Goiânia',
     'Completed',
     'Active',
     'Final', 'Forest',
     356.7890, 1024.5678, 78.9012,
     'Fully Mapped',
     245.6789, 230.1234, 25.6789,
     215.4321, 210.9876,
     98.7654, 65.4321, 55.6789,
     70.2345, -289.0123,
     45.6789, 35.4321, 25.6789,
     'No Active Restrictions',
     '2024-10-27',
     987.6543, 63.8901,
     'National Park - CHAPADA DOS VEADEIROS',
     '2024-10-27',
     1546.2981, 100.0),
     ('4567538769','GO-5220009-12A3.B456.7890.1234.DEF5.6789.0123.ABCD',
      '2024-10-25 16:40:31',
      '2024-10-25',
      856.4321,
      '15°45''23.12\" S',
      '47°18''45.32\" W',
      'Pirenópolis',
      'Em análise',
      'Ativo',
      '', '', 0.0, 0.0, 0.0,
      'Reserva Legal não analisada',
      0.0, 0.0, 0.0, 0.0, 0.0,
      0.0, 0.0, 0.0, 0.0,
      -156.7890, 0.0, 0.0, 0.0,
      'Infração: Infração contra Flora (Não classificado-Móvel)',
      '2024-10-25',
      523.4567, 61.1234,
      'Área de Proteção Ambiental - APA CHAPADA DOS VEADEIROS',
      '2024-10-25',
      856.4321, 100.0),
      ('4567538771','GO-5220009-12A3.B456.7890.1234.DEF5.6789.0123.ABCD',
       '2024-10-25 16:40:31',
       '2024-10-25',
       856.4321,
       '15°45''23.12\" S',
       '47°18''45.32\" W',
       'Pirenópolis',
       'Em análise',
       'Ativo',
       '', '', 0.0, 0.0, 0.0,
       'Reserva Legal não analisada',
       0.0, 0.0, 0.0, 0.0, 0.0,
       0.0, 0.0, 0.0, 0.0,
       -156.7890, 0.0, 0.0, 0.0,
       'Infração: Infração contra Flora (Não classificado-Móvel)',
       '2024-10-25',
       523.4567, 61.1234,
       'Área de Proteção Ambiental - APA CHAPADA DOS VEADEIROS',
       '2024-10-25',
       856.4321, 100.0);
