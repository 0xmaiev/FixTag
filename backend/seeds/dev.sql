-- Demo data for development and the class demo. Safe to run more than once.
-- Every user's password is "fixtag123", generate other hashes with `fixtag-api hash-password`.
-- The QR codes on the machines encode their serial number: FT-0001 to FT-0008 at Cliente Demo,
-- FT-0101 to FT-0103 and CA-2001 to CA-2002 at Metalúrgica Norte.
--
-- Two manufacturers: Fabricante Demo serves both clients, Compressores Atlântico only
-- Metalúrgica Norte, which ends up with machines from both. Each manufacturer's staff only see
-- the machines it made, while Metalúrgica Norte's staff see all of them.
--
-- The ids are the same in the app's demo flavor (DemoSeed.kt). Each kind
-- of row has its own first digit: a organizations, b users, c machines, d incidents, e updates.
--
-- Report and history times are relative to when the seed runs. updated_at isn't backdated, so
-- every seeded incident counts as changed now and an app that synced before still gets them.
-- Compressor FT-0003 has no incidents on purpose, the API tests count on that.

INSERT INTO organizations (id, name, kind, nif) VALUES
    ('a0000000-0000-4000-8000-000000000001', 'Cliente Demo, Lda.',            'cliente',    '500000018'),
    ('a0000000-0000-4000-8000-000000000002', 'Fabricante Demo, S.A.',         'fabricante', '500000026'),
    ('a0000000-0000-4000-8000-000000000003', 'Metalúrgica Norte, S.A.',       'cliente',    '500000034'),
    ('a0000000-0000-4000-8000-000000000004', 'Compressores Atlântico, Lda.',  'fabricante', '500000042')
ON CONFLICT (id) DO NOTHING;

-- Which manufacturer serves which client.
INSERT INTO client_manufacturers (client_id, manufacturer_id) VALUES
    ('a0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000002'),
    ('a0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000002'),
    ('a0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000004')
ON CONFLICT DO NOTHING;

-- Clients have operators who report problems and a gestor who creates their accounts. Each
-- manufacturer has support staff who take the reports and hand each one to one of its
-- technicians, and a gestor who can do all of that and creates the accounts.
INSERT INTO users (id, organization_id, organization_kind, name, email, password_hash, role)
SELECT u.id::uuid, o.id, o.kind, u.name, u.email,
       '$argon2id$v=19$m=19456,t=2,p=1$PrjFspsfPQgVo3BNWaIXtg$81xmd8yeZ2GX0+kTD7VSKJNHKTbdJCFwuy1Hrw5kktI',
       u.role::user_role
  FROM (VALUES
    -- Cliente Demo
    ('b0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001', 'Ana Operadora',      'operador@fixtag.test',     'operador'),
    ('b0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000001', 'Diogo Operador',     'operador2@fixtag.test',    'operador'),
    ('b0000000-0000-4000-8000-000000000009', 'a0000000-0000-4000-8000-000000000001', 'Isabel Gestora',     'gestor2@fixtag.test',      'gestor'),
    -- Metalúrgica Norte
    ('b0000000-0000-4000-8000-000000000007', 'a0000000-0000-4000-8000-000000000003', 'Gabriela Operadora', 'operador3@fixtag.test',    'operador'),
    ('b0000000-0000-4000-8000-000000000010', 'a0000000-0000-4000-8000-000000000003', 'João Gestor',        'gestor3@fixtag.test',      'gestor'),
    -- Fabricante Demo
    ('b0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000002', 'Carla Assistência',  'assistencia@fixtag.test',  'assistencia'),
    ('b0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000002', 'Bruno Técnico',      'tecnico@fixtag.test',      'tecnico'),
    ('b0000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000002', 'Eva Técnica',        'tecnico2@fixtag.test',     'tecnico'),
    ('b0000000-0000-4000-8000-000000000006', 'a0000000-0000-4000-8000-000000000002', 'Filipe Técnico',     'tecnico3@fixtag.test',     'tecnico'),
    ('b0000000-0000-4000-8000-000000000008', 'a0000000-0000-4000-8000-000000000002', 'Helena Gestora',     'gestor@fixtag.test',       'gestor'),
    -- Compressores Atlântico
    ('b0000000-0000-4000-8000-000000000011', 'a0000000-0000-4000-8000-000000000004', 'Luísa Assistência',  'assistencia2@fixtag.test', 'assistencia'),
    ('b0000000-0000-4000-8000-000000000012', 'a0000000-0000-4000-8000-000000000004', 'Marco Técnico',      'tecnico4@fixtag.test',     'tecnico'),
    ('b0000000-0000-4000-8000-000000000013', 'a0000000-0000-4000-8000-000000000004', 'Nuno Gestor',        'gestor4@fixtag.test',      'gestor')
  ) AS u (id, organization_id, name, email, role)
  JOIN organizations o ON o.id = u.organization_id::uuid
ON CONFLICT (id) DO NOTHING;

INSERT INTO machines (id, organization_id, manufacturer_id, serial_number, model) VALUES
    -- Cliente Demo, made by Fabricante Demo
    ('c0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000002', 'FT-0001', 'Prensa hidráulica PH-200'),
    ('c0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000002', 'FT-0002', 'Torno CNC TC-450'),
    ('c0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000002', 'FT-0003', 'Compressor CP-90'),
    ('c0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000002', 'FT-0004', 'Fresadora CNC FV-320'),
    ('c0000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000002', 'FT-0005', 'Robot de soldadura RS-6'),
    ('c0000000-0000-4000-8000-000000000006', 'a0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000002', 'FT-0006', 'Guilhotina hidráulica GH-3000'),
    ('c0000000-0000-4000-8000-000000000007', 'a0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000002', 'FT-0007', 'Quinadora QP-135'),
    ('c0000000-0000-4000-8000-000000000008', 'a0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000002', 'FT-0008', 'Serra de fita SF-460'),
    -- Metalúrgica Norte, made by Fabricante Demo
    ('c0000000-0000-4000-8000-000000000009', 'a0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000002', 'FT-0101', 'Centro de maquinação CM-800'),
    ('c0000000-0000-4000-8000-000000000010', 'a0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000002', 'FT-0102', 'Laser de corte LC-3015'),
    ('c0000000-0000-4000-8000-000000000011', 'a0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000002', 'FT-0103', 'Compressor de parafuso CP-150'),
    -- Metalúrgica Norte, made by Compressores Atlântico
    ('c0000000-0000-4000-8000-000000000012', 'a0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000004', 'CA-2001', 'Compressor de parafuso CA-55'),
    ('c0000000-0000-4000-8000-000000000013', 'a0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000004', 'CA-2002', 'Secador de ar comprimido SD-40')
ON CONFLICT (id) DO NOTHING;

-- `ago` is how long ago the operator reported it, `synced_after` how long the app took to send
-- it (FT-0002 and FT-0103 were reported offline). The client and the manufacturer handling it
-- come from the machine, status and technician from the history.
INSERT INTO incidents (id, organization_id, handled_by, machine_id, author_id, description,
                       severity, latitude, longitude, accuracy_m, reported_at, created_at)
SELECT i.id::uuid, m.organization_id, m.manufacturer_id, m.id, i.author_id::uuid, i.description,
       i.severity::incident_severity, i.latitude, i.longitude, i.accuracy_m,
       now() - i.ago::interval, now() - i.ago::interval + i.synced_after::interval
  FROM (VALUES
    -- Cliente Demo
    ('d0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000001',
     'Fuga de óleo no cilindro principal', 'critica', 41.1579, -8.6291, 12, '2 days', '1 minute'),
    ('d0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000004',
     'Ruído anormal no eixo durante o arranque', 'media', NULL, NULL, NULL, '1 day', '3 hours'),
    ('d0000000-0000-4000-8000-000000000003', 'c0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000001',
     'A iluminação interna da cabine não acende', 'baixa', 41.1580, -8.6288, 20, '6 days', '2 minutes'),
    ('d0000000-0000-4000-8000-000000000004', 'c0000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000004',
     'O robot pára a meio do ciclo com erro de servo no eixo 3', 'critica', 41.1577, -8.6295, 8, '3 hours', '1 minute'),
    ('d0000000-0000-4000-8000-000000000005', 'c0000000-0000-4000-8000-000000000006', 'b0000000-0000-4000-8000-000000000001',
     'Lâmina desalinhada, os cortes saem com rebarba', 'media', NULL, NULL, NULL, '4 days', '5 minutes'),
    ('d0000000-0000-4000-8000-000000000006', 'c0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000004',
     'Manómetro com leitura instável', 'baixa', 41.1579, -8.6291, 15, '10 days', '1 minute'),
    ('d0000000-0000-4000-8000-000000000007', 'c0000000-0000-4000-8000-000000000007', 'b0000000-0000-4000-8000-000000000001',
     'O pedal de comando não responde, máquina parada', 'critica', 41.1581, -8.6290, 10, '40 minutes', '1 minute'),
    ('d0000000-0000-4000-8000-000000000008', 'c0000000-0000-4000-8000-000000000008', 'b0000000-0000-4000-8000-000000000004',
     'A fita parte com frequência', 'media', NULL, NULL, NULL, '2 days 6 hours', '20 minutes'),
    -- Metalúrgica Norte, Fabricante Demo's machines
    ('d0000000-0000-4000-8000-000000000009', 'c0000000-0000-4000-8000-000000000009', 'b0000000-0000-4000-8000-000000000007',
     'Alarme de sobreaquecimento no fuso', 'critica', 41.5454, -8.4265, 25, '5 hours', '2 minutes'),
    ('d0000000-0000-4000-8000-000000000010', 'c0000000-0000-4000-8000-000000000010', 'b0000000-0000-4000-8000-000000000007',
     'Qualidade de corte irregular nas chapas de inox', 'media', 41.5456, -8.4261, 30, '3 days', '1 minute'),
    ('d0000000-0000-4000-8000-000000000011', 'c0000000-0000-4000-8000-000000000011', 'b0000000-0000-4000-8000-000000000007',
     'A purga automática de condensados não funciona', 'baixa', NULL, NULL, NULL, '8 days', '4 hours'),
    ('d0000000-0000-4000-8000-000000000012', 'c0000000-0000-4000-8000-000000000009', 'b0000000-0000-4000-8000-000000000007',
     'A porta de proteção não fecha bem', 'baixa', 41.5454, -8.4265, 18, '12 days', '1 minute'),
    -- Metalúrgica Norte, Compressores Atlântico's machines
    ('d0000000-0000-4000-8000-000000000013', 'c0000000-0000-4000-8000-000000000012', 'b0000000-0000-4000-8000-000000000007',
     'Pressão de saída abaixo do normal', 'media', 41.5455, -8.4263, 15, '1 day 2 hours', '1 minute'),
    ('d0000000-0000-4000-8000-000000000014', 'c0000000-0000-4000-8000-000000000013', 'b0000000-0000-4000-8000-000000000010',
     'Fuga de ar na ligação ao depósito', 'baixa', NULL, NULL, NULL, '4 hours', '2 minutes')
  ) AS i (id, machine_id, author_id, description, severity, latitude, longitude, accuracy_m, ago,
          synced_after)
  JOIN machines m ON m.id = i.machine_id::uuid
ON CONFLICT (id) DO NOTHING;

-- Inserted oldest first, so the incident_updates_after_insert trigger leaves each incident with
-- the status and technician of its latest entry. FT-0002, FT-0007 and FT-0101 are still waiting
-- for support to pick a technician, and CA-2002 for Compressores Atlântico's. Bruno has 3 open
-- incidents, Filipe 2, Eva none and Marco 1.
INSERT INTO incident_updates (id, incident_id, author_id, message, new_status, assigned_to,
                              created_at)
SELECT u.id::uuid, u.incident_id::uuid, u.author_id::uuid, u.message,
       u.new_status::incident_status, u.assigned_to::uuid, now() - u.ago::interval
  FROM (VALUES
    -- FT-0001, oil leak: Carla sends Bruno
    ('e0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000003',
     'Técnico agendado para amanhã de manhã', 'em_analise', 'b0000000-0000-4000-8000-000000000002', '1 day 22 hours'),
    ('e0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000002',
     'Vedante do cilindro danificado, fica substituído hoje.', NULL, NULL, '1 day 2 hours'),
    -- FT-0004, cabin light: fixed by Eva
    ('e0000000-0000-4000-8000-000000000003', 'd0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000003',
     'A Eva passa aí esta semana.', 'em_analise', 'b0000000-0000-4000-8000-000000000005', '5 days 20 hours'),
    ('e0000000-0000-4000-8000-000000000004', 'd0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000005',
     'Lâmpada LED fundida, já substituída.', NULL, NULL, '4 days 3 hours'),
    ('e0000000-0000-4000-8000-000000000005', 'd0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000003',
     'Confirmado com o cliente, fica resolvida.', 'resolvida', NULL, '4 days 1 hour'),
    -- FT-0005, welding robot: assigned to Filipe, not started yet
    ('e0000000-0000-4000-8000-000000000006', 'd0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000003',
     'O Filipe é o técnico mais próximo, segue para lá.', NULL, 'b0000000-0000-4000-8000-000000000006', '2 hours'),
    ('e0000000-0000-4000-8000-000000000022', 'd0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000006',
     'A caminho, chego dentro de uma hora.', NULL, NULL, '1 hour 30 minutes'),
    -- FT-0006, guillotine blade: the operator adds details, then Bruno is assigned
    ('e0000000-0000-4000-8000-000000000007', 'd0000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000001',
     'Acontece sobretudo com chapa acima de 3 mm.', NULL, NULL, '3 days 23 hours'),
    ('e0000000-0000-4000-8000-000000000008', 'd0000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000003',
     NULL, 'em_analise', 'b0000000-0000-4000-8000-000000000002', '3 days 2 hours'),
    ('e0000000-0000-4000-8000-000000000009', 'd0000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000002',
     'É preciso afinar a folga da lâmina, volto amanhã com a ferramenta certa.', NULL, NULL, '2 days 5 hours'),
    -- FT-0001, pressure gauge: an older incident on the same press, fixed by Bruno
    ('e0000000-0000-4000-8000-000000000010', 'd0000000-0000-4000-8000-000000000006', 'b0000000-0000-4000-8000-000000000003',
     NULL, 'em_analise', 'b0000000-0000-4000-8000-000000000002', '9 days 22 hours'),
    ('e0000000-0000-4000-8000-000000000011', 'd0000000-0000-4000-8000-000000000006', 'b0000000-0000-4000-8000-000000000002',
     'Manómetro substituído.', NULL, NULL, '9 days 3 hours'),
    ('e0000000-0000-4000-8000-000000000012', 'd0000000-0000-4000-8000-000000000006', 'b0000000-0000-4000-8000-000000000003',
     'Sem novas leituras instáveis, fica resolvida.', 'resolvida', NULL, '9 days 1 hour'),
    -- FT-0008, band saw
    ('e0000000-0000-4000-8000-000000000013', 'd0000000-0000-4000-8000-000000000008', 'b0000000-0000-4000-8000-000000000003',
     'Verificar a tensão da fita e as guias.', 'em_analise', 'b0000000-0000-4000-8000-000000000002', '1 day 20 hours'),
    -- FT-0102, laser cutter
    ('e0000000-0000-4000-8000-000000000014', 'd0000000-0000-4000-8000-000000000010', 'b0000000-0000-4000-8000-000000000003',
     'O Filipe está na zona de Braga esta semana.', 'em_analise', 'b0000000-0000-4000-8000-000000000006', '2 days 20 hours'),
    ('e0000000-0000-4000-8000-000000000015', 'd0000000-0000-4000-8000-000000000010', 'b0000000-0000-4000-8000-000000000006',
     'Lente de focagem suja, limpeza feita. Vou acompanhar mais uns dias.', NULL, NULL, '1 day 4 hours'),
    -- FT-0103, condensate drain: fixed by Eva
    ('e0000000-0000-4000-8000-000000000016', 'd0000000-0000-4000-8000-000000000011', 'b0000000-0000-4000-8000-000000000003',
     NULL, 'em_analise', 'b0000000-0000-4000-8000-000000000005', '7 days 18 hours'),
    ('e0000000-0000-4000-8000-000000000017', 'd0000000-0000-4000-8000-000000000011', 'b0000000-0000-4000-8000-000000000005',
     'Válvula de purga obstruída, limpa e testada.', NULL, NULL, '7 days 2 hours'),
    ('e0000000-0000-4000-8000-000000000018', 'd0000000-0000-4000-8000-000000000011', 'b0000000-0000-4000-8000-000000000003',
     NULL, 'resolvida', NULL, '7 days 1 hour'),
    -- FT-0101, safety door: fixed by Bruno
    ('e0000000-0000-4000-8000-000000000019', 'd0000000-0000-4000-8000-000000000012', 'b0000000-0000-4000-8000-000000000003',
     NULL, 'em_analise', 'b0000000-0000-4000-8000-000000000002', '11 days 20 hours'),
    ('e0000000-0000-4000-8000-000000000020', 'd0000000-0000-4000-8000-000000000012', 'b0000000-0000-4000-8000-000000000002',
     'Dobradiça ajustada, a porta já fecha.', NULL, NULL, '11 days 3 hours'),
    ('e0000000-0000-4000-8000-000000000021', 'd0000000-0000-4000-8000-000000000012', 'b0000000-0000-4000-8000-000000000003',
     NULL, 'resolvida', NULL, '11 days 2 hours'),
    -- CA-2001, outlet pressure: Compressores Atlântico's Luísa sends Marco
    ('e0000000-0000-4000-8000-000000000023', 'd0000000-0000-4000-8000-000000000013', 'b0000000-0000-4000-8000-000000000011',
     'O Marco passa aí amanhã de manhã.', 'em_analise', 'b0000000-0000-4000-8000-000000000012', '1 day'),
    ('e0000000-0000-4000-8000-000000000024', 'd0000000-0000-4000-8000-000000000013', 'b0000000-0000-4000-8000-000000000012',
     'Filtro de admissão colmatado, trago um novo amanhã.', NULL, NULL, '3 hours')
  ) AS u (id, incident_id, author_id, message, new_status, assigned_to, ago)
 ORDER BY u.ago::interval DESC
ON CONFLICT (id) DO NOTHING;
