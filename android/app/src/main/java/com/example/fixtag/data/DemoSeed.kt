package com.example.fixtag.data

import com.example.fixtag.data.model.ClientManufacturerLink
import com.example.fixtag.data.model.Incident
import com.example.fixtag.data.model.IncidentSeverity
import com.example.fixtag.data.model.IncidentStatus
import com.example.fixtag.data.model.IncidentUpdate
import com.example.fixtag.data.model.Machine
import com.example.fixtag.data.model.Organization
import com.example.fixtag.data.model.OrganizationKind
import com.example.fixtag.data.model.SyncStatus
import com.example.fixtag.data.model.User
import com.example.fixtag.data.model.UserRole
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.Uuid

// Demo variant only: what the server would have sent. Same ids as backend/seeds/dev.sql.
object DemoSeed {

    // organizations

    val CLIENT_1 = Uuid.parse("a0000000-0000-4000-8000-000000000001")
    const val CLIENT_1_NIF = "500000018"
    const val CLIENT_1_NAME = "Cliente Demo, Lda."

    val MANUFACTURER_1 = Uuid.parse("a0000000-0000-4000-8000-000000000002")
    const val MANUFACTURER_1_NIF = "500000026"
    const val MANUFACTURER_1_NAME = "Fabricante Demo, S.A."

    val CLIENT_2 = Uuid.parse("a0000000-0000-4000-8000-000000000003")
    const val CLIENT_2_NIF = "500000034"
    const val CLIENT_2_NAME = "Metalúrgica Norte, S.A."

    val MANUFACTURER_2 = Uuid.parse("a0000000-0000-4000-8000-000000000004")
    const val MANUFACTURER_2_NIF = "500000042"
    const val MANUFACTURER_2_NAME = "Compressores Atlântico, Lda."

    // users
    // Cliente Demo
    val OPERATOR_1 = Uuid.parse("b0000000-0000-4000-8000-000000000001")
    const val OPERATOR_1_NAME = "Ana Operadora"
    const val OPERATOR_1_EMAIL = "operador@fixtag.test"
    val OPERATOR_2 = Uuid.parse("b0000000-0000-4000-8000-000000000004")
    const val OPERATOR_2_NAME = "Diogo Operador"
    const val OPERATOR_2_EMAIL = "operador2@fixtag.test"
    val MANAGER_2 = Uuid.parse("b0000000-0000-4000-8000-000000000009")
    const val MANAGER_2_NAME = "Isabel Gestora"
    const val MANAGER_2_EMAIL = "gestor2@fixtag.test"

    // Metalúrgica Norte
    val OPERATOR_3 = Uuid.parse("b0000000-0000-4000-8000-000000000007")
    const val OPERATOR_3_NAME = "Gabriela Operadora"
    const val OPERATOR_3_EMAIL = "operador3@fixtag.test"
    val MANAGER_3 = Uuid.parse("b0000000-0000-4000-8000-000000000010")
    const val MANAGER_3_NAME = "João Gestor"
    const val MANAGER_3_EMAIL = "gestor3@fixtag.test"

    // Fabricante Demo
    val ASSISTANCE_1 = Uuid.parse("b0000000-0000-4000-8000-000000000003")
    const val ASSISTANCE_1_NAME = "Carla Assistência"
    const val ASSISTANCE_1_EMAIL = "assistencia@fixtag.test"
    val TECHNICIAN_1 = Uuid.parse("b0000000-0000-4000-8000-000000000002")
    const val TECHNICIAN_1_NAME = "Bruno Técnico"
    const val TECHNICIAN_1_EMAIL = "tecnico@fixtag.test"
    val TECHNICIAN_2 = Uuid.parse("b0000000-0000-4000-8000-000000000005")
    const val TECHNICIAN_2_NAME = "Eva Técnica"
    const val TECHNICIAN_2_EMAIL = "tecnico2@fixtag.test"
    val TECHNICIAN_3 = Uuid.parse("b0000000-0000-4000-8000-000000000006")
    const val TECHNICIAN_3_NAME = "Filipe Técnico"
    const val TECHNICIAN_3_EMAIL = "tecnico3@fixtag.test"
    val MANAGER_1 = Uuid.parse("b0000000-0000-4000-8000-000000000008")
    const val MANAGER_1_NAME = "Helena Gestora"
    const val MANAGER_1_EMAIL = "gestor@fixtag.test"

    // Compressores Atlântico
    val ASSISTANCE_2 = Uuid.parse("b0000000-0000-4000-8000-000000000011")
    const val ASSISTANCE_2_NAME = "Luísa Assistência"
    const val ASSISTANCE_2_EMAIL = "assistencia2@fixtag.test"
    val TECHNICIAN_4 = Uuid.parse("b0000000-0000-4000-8000-000000000012")
    const val TECHNICIAN_4_NAME = "Marco Técnico"
    const val TECHNICIAN_4_EMAIL = "tecnico4@fixtag.test"
    val MANAGER_4 = Uuid.parse("b0000000-0000-4000-8000-000000000013")
    const val MANAGER_4_NAME = "Nuno Gestor"
    const val MANAGER_4_EMAIL = "gestor4@fixtag.test"

    // machines

    val MACHINE_1 = Uuid.parse("c0000000-0000-4000-8000-000000000001")
    const val MACHINE_1_MODEL = "Prensa hidráulica PH-200"
    const val MACHINE_1_SN = "FT-0001"
    val MACHINE_2 = Uuid.parse("c0000000-0000-4000-8000-000000000002")
    const val MACHINE_2_MODEL = "Torno CNC TC-450"
    const val MACHINE_2_SN = "FT-0002"
    val MACHINE_3 = Uuid.parse("c0000000-0000-4000-8000-000000000003")
    const val MACHINE_3_MODEL = "Compressor CP-90"
    const val MACHINE_3_SN = "FT-0003"
    val MACHINE_4 = Uuid.parse("c0000000-0000-4000-8000-000000000004")
    const val MACHINE_4_MODEL = "Fresadora CNC FV-320"
    const val MACHINE_4_SN = "FT-0004"
    val MACHINE_5 = Uuid.parse("c0000000-0000-4000-8000-000000000005")
    const val MACHINE_5_MODEL = "Robot de soldadura RS-6"
    const val MACHINE_5_SN = "FT-0005"
    val MACHINE_6 = Uuid.parse("c0000000-0000-4000-8000-000000000006")
    const val MACHINE_6_MODEL = "Guilhotina hidráulica GH-3000"
    const val MACHINE_6_SN = "FT-0006"
    val MACHINE_7 = Uuid.parse("c0000000-0000-4000-8000-000000000007")
    const val MACHINE_7_MODEL = "Quinadora QP-135"
    const val MACHINE_7_SN = "FT-0007"
    val MACHINE_8 = Uuid.parse("c0000000-0000-4000-8000-000000000008")
    const val MACHINE_8_MODEL = "Serra de fita SF-460"
    const val MACHINE_8_SN = "FT-0008"
    val MACHINE_9 = Uuid.parse("c0000000-0000-4000-8000-000000000009")
    const val MACHINE_9_MODEL = "Centro de maquinação CM-800"
    const val MACHINE_9_SN = "FT-0101"
    val MACHINE_10 = Uuid.parse("c0000000-0000-4000-8000-000000000010")
    const val MACHINE_10_MODEL = "Laser de corte LC-3015"
    const val MACHINE_10_SN = "FT-0102"
    val MACHINE_11 = Uuid.parse("c0000000-0000-4000-8000-000000000011")
    const val MACHINE_11_MODEL = "Compressor de parafuso CP-150"
    const val MACHINE_11_SN = "FT-0103"
    val MACHINE_12 = Uuid.parse("c0000000-0000-4000-8000-000000000012")
    const val MACHINE_12_MODEL = "Compressor de parafuso CA-55"
    const val MACHINE_12_SN = "CA-2001"
    val MACHINE_13 = Uuid.parse("c0000000-0000-4000-8000-000000000013")
    const val MACHINE_13_MODEL = "Secador de ar comprimido SD-40"
    const val MACHINE_13_SN = "CA-2002"

    // incidents
    val INCIDENT_1 = Uuid.parse("d0000000-0000-4000-8000-000000000001")
    const val INCIDENT_1_DESC = "Fuga de óleo no cilindro principal"
    val INCIDENT_2 = Uuid.parse("d0000000-0000-4000-8000-000000000002")
    const val INCIDENT_2_DESC = "Ruído anormal no eixo durante o arranque"
    val INCIDENT_3 = Uuid.parse("d0000000-0000-4000-8000-000000000003")
    const val INCIDENT_3_DESC = "A iluminação interna da cabine não acende"
    val INCIDENT_4 = Uuid.parse("d0000000-0000-4000-8000-000000000004")
    const val INCIDENT_4_DESC = "O robot pára a meio do ciclo com erro de servo no eixo 3"
    val INCIDENT_5 = Uuid.parse("d0000000-0000-4000-8000-000000000005")
    const val INCIDENT_5_DESC = "Lâmina desalinhada, os cortes saem com rebarba"
    val INCIDENT_6 = Uuid.parse("d0000000-0000-4000-8000-000000000006")
    const val INCIDENT_6_DESC = "Manómetro com leitura instável"
    val INCIDENT_7 = Uuid.parse("d0000000-0000-4000-8000-000000000007")
    const val INCIDENT_7_DESC = "O pedal de comando não responde, máquina parada"
    val INCIDENT_8 = Uuid.parse("d0000000-0000-4000-8000-000000000008")
    const val INCIDENT_8_DESC = "A fita parte com frequência"
    val INCIDENT_9 = Uuid.parse("d0000000-0000-4000-8000-000000000009")
    const val INCIDENT_9_DESC = "Alarme de sobreaquecimento no fuso"
    val INCIDENT_10 = Uuid.parse("d0000000-0000-4000-8000-000000000010")
    const val INCIDENT_10_DESC = "Qualidade de corte irregular nas chapas de inox"
    val INCIDENT_11 = Uuid.parse("d0000000-0000-4000-8000-000000000011")
    const val INCIDENT_11_DESC = "A purga automática de condensados não funciona"
    val INCIDENT_12 = Uuid.parse("d0000000-0000-4000-8000-000000000012")
    const val INCIDENT_12_DESC = "A porta de proteção não fecha bem"
    val INCIDENT_13 = Uuid.parse("d0000000-0000-4000-8000-000000000013")
    const val INCIDENT_13_DESC = "Pressão de saída abaixo do normal"
    val INCIDENT_14 = Uuid.parse("d0000000-0000-4000-8000-000000000014")
    const val INCIDENT_14_DESC = "Fuga de ar na ligação ao depósito"
    
    // updates
    val UPDATE_1 = Uuid.parse("e0000000-0000-4000-8000-000000000001")
    const val UPDATE_1_MESSAGE = "Técnico agendado para amanhã de manhã"

    val UPDATE_2 = Uuid.parse("e0000000-0000-4000-8000-000000000002")
    const val UPDATE_2_MESSAGE = "Vedante do cilindro danificado, fica substituído hoje."

    val UPDATE_3 = Uuid.parse("e0000000-0000-4000-8000-000000000003")
    const val UPDATE_3_MESSAGE = "A Eva passa aí esta semana."

    val UPDATE_4 = Uuid.parse("e0000000-0000-4000-8000-000000000004")
    const val UPDATE_4_MESSAGE = "Lâmpada LED fundida, já substituída."
    val UPDATE_5 = Uuid.parse("e0000000-0000-4000-8000-000000000005")
    const val UPDATE_5_MESSAGE = "Confirmado com o cliente, fica resolvida."
    val UPDATE_6 = Uuid.parse("e0000000-0000-4000-8000-000000000006")
    const val UPDATE_6_MESSAGE = "O Filipe é o técnico mais próximo, segue para lá."
    val UPDATE_7 = Uuid.parse("e0000000-0000-4000-8000-000000000007")
    const val UPDATE_7_MESSAGE = "Acontece sobretudo com chapa acima de 3 mm."
    val UPDATE_8 = Uuid.parse("e0000000-0000-4000-8000-000000000008")
    val UPDATE_9 = Uuid.parse("e0000000-0000-4000-8000-000000000009")
    const val UPDATE_9_MESSAGE = "É preciso afinar a folga da lâmina, volto amanhã com a ferramenta certa."
    val UPDATE_10 = Uuid.parse("e0000000-0000-4000-8000-000000000010")
    val UPDATE_11 = Uuid.parse("e0000000-0000-4000-8000-000000000011")
    const val UPDATE_11_MESSAGE = "Manómetro substituído."
    val UPDATE_12 = Uuid.parse("e0000000-0000-4000-8000-000000000012")
    const val UPDATE_12_MESSAGE = "Sem novas leituras instáveis, fica resolvida."
    val UPDATE_13 = Uuid.parse("e0000000-0000-4000-8000-000000000013")
    const val UPDATE_13_MESSAGE = "Verificar a tensão da fita e as guias."
    val UPDATE_14 = Uuid.parse("e0000000-0000-4000-8000-000000000014")
    const val UPDATE_14_MESSAGE = "O Filipe está na zona de Braga esta semana."
    val UPDATE_15 = Uuid.parse("e0000000-0000-4000-8000-000000000015")
    const val UPDATE_15_MESSAGE = "Lente de focagem suja, limpeza feita. Vou acompanhar mais uns dias."
    val UPDATE_16 = Uuid.parse("e0000000-0000-4000-8000-000000000016")
    val UPDATE_17 = Uuid.parse("e0000000-0000-4000-8000-000000000017")
    const val UPDATE_17_MESSAGE = "Válvula de purga obstruída, limpa e testada."
    val UPDATE_18 = Uuid.parse("e0000000-0000-4000-8000-000000000018")
    val UPDATE_19 = Uuid.parse("e0000000-0000-4000-8000-000000000019")
    val UPDATE_20 = Uuid.parse("e0000000-0000-4000-8000-000000000020")
    const val UPDATE_20_MESSAGE = "Dobradiça ajustada, a porta já fecha."
    val UPDATE_21 = Uuid.parse("e0000000-0000-4000-8000-000000000021")
    val UPDATE_22 = Uuid.parse("e0000000-0000-4000-8000-000000000022")
    const val UPDATE_22_MESSAGE = "A caminho, chego dentro de uma hora."
    val UPDATE_23 = Uuid.parse("e0000000-0000-4000-8000-000000000023")
    const val UPDATE_23_MESSAGE = "O Marco passa aí amanhã de manhã."
    val UPDATE_24 = Uuid.parse("e0000000-0000-4000-8000-000000000024")
    const val UPDATE_24_MESSAGE = "Filtro de admissão colmatado, trago um novo amanhã."

    fun organizations(now: Instant) = listOf(
        Organization(
            CLIENT_1,
            CLIENT_1_NAME,
            OrganizationKind.Cliente,
            now,
            SyncStatus.Synced,
            CLIENT_1_NIF
        ),
        Organization(
            MANUFACTURER_1,
            MANUFACTURER_1_NAME,
            OrganizationKind.Fabricante,
            now,
            SyncStatus.Synced,
            MANUFACTURER_1_NIF
        ),
        Organization(
            CLIENT_2,
            CLIENT_2_NAME,
            OrganizationKind.Cliente,
            now,
            SyncStatus.Synced,
            CLIENT_2_NIF
        ),
        Organization(
            MANUFACTURER_2,
            MANUFACTURER_2_NAME,
            OrganizationKind.Fabricante,
            now,
            SyncStatus.Synced,
            MANUFACTURER_2_NIF
        ),
    )

    // which manufacturer serves which client, Metalúrgica Norte buys from both
    fun links(now: Instant) = listOf(
        ClientManufacturerLink(
            CLIENT_1,
            MANUFACTURER_1,
            now,
            SyncStatus.Synced
        ),
        ClientManufacturerLink(
            CLIENT_2,
            MANUFACTURER_1,
            now,
            SyncStatus.Synced
        ),
        ClientManufacturerLink(
            CLIENT_2,
            MANUFACTURER_2,
            now,
            SyncStatus.Synced
        ),
    )

    val users = listOf(
        // Cliente Demo
        User(
            OPERATOR_1,
            CLIENT_1,
            OrganizationKind.Cliente,
            OPERATOR_1_NAME,
            OPERATOR_1_EMAIL,
            UserRole.Operador,
            false,
            SyncStatus.Synced
        ),
        User(
            OPERATOR_2,
            CLIENT_1,
            OrganizationKind.Cliente,
            OPERATOR_2_NAME,
            OPERATOR_2_EMAIL,
            UserRole.Operador,
            false,
            SyncStatus.Synced
        ),
        User(
            MANAGER_2,
            CLIENT_1,
            OrganizationKind.Cliente,
            MANAGER_2_NAME,
            MANAGER_2_EMAIL,
            UserRole.Gestor,
            false,
            SyncStatus.Synced
        ),
        // Metalúrgica Norte
        User(
            OPERATOR_3,
            CLIENT_2,
            OrganizationKind.Cliente,
            OPERATOR_3_NAME,
            OPERATOR_3_EMAIL,
            UserRole.Operador,
            false,
            SyncStatus.Synced
        ),
        User(
            MANAGER_3,
            CLIENT_2,
            OrganizationKind.Cliente,
            MANAGER_3_NAME,
            MANAGER_3_EMAIL,
            UserRole.Gestor,
            false,
            SyncStatus.Synced
        ),
        // Fabricante Demo
        User(
            ASSISTANCE_1,
            MANUFACTURER_1,
            OrganizationKind.Fabricante,
            ASSISTANCE_1_NAME,
            ASSISTANCE_1_EMAIL,
            UserRole.Assistencia,
            false,
            SyncStatus.Synced
        ),
        User(
            TECHNICIAN_1,
            MANUFACTURER_1,
            OrganizationKind.Fabricante,
            TECHNICIAN_1_NAME,
            TECHNICIAN_1_EMAIL,
            UserRole.Tecnico,
            false,
            SyncStatus.Synced
        ),
        User(
            TECHNICIAN_2,
            MANUFACTURER_1,
            OrganizationKind.Fabricante,
            TECHNICIAN_2_NAME,
            TECHNICIAN_2_EMAIL,
            UserRole.Tecnico,
            false,
            SyncStatus.Synced
        ),
        User(
            TECHNICIAN_3,
            MANUFACTURER_1,
            OrganizationKind.Fabricante,
            TECHNICIAN_3_NAME,
            TECHNICIAN_3_EMAIL,
            UserRole.Tecnico,
            false,
            SyncStatus.Synced
        ),
        User(
            MANAGER_1,
            MANUFACTURER_1,
            OrganizationKind.Fabricante,
            MANAGER_1_NAME,
            MANAGER_1_EMAIL,
            UserRole.Gestor,
            false,
            SyncStatus.Synced
        ),
        // Compressores Atlântico
        User(
            ASSISTANCE_2,
            MANUFACTURER_2,
            OrganizationKind.Fabricante,
            ASSISTANCE_2_NAME,
            ASSISTANCE_2_EMAIL,
            UserRole.Assistencia,
            false,
            SyncStatus.Synced
        ),
        User(
            TECHNICIAN_4,
            MANUFACTURER_2,
            OrganizationKind.Fabricante,
            TECHNICIAN_4_NAME,
            TECHNICIAN_4_EMAIL,
            UserRole.Tecnico,
            false,
            SyncStatus.Synced
        ),
        User(
            MANAGER_4,
            MANUFACTURER_2,
            OrganizationKind.Fabricante,
            MANAGER_4_NAME,
            MANAGER_4_EMAIL,
            UserRole.Gestor,
            false,
            SyncStatus.Synced
        ),
    )

    fun machines(now: Instant) = listOf(
        Machine(
            MACHINE_1,
            CLIENT_1,
            MANUFACTURER_1,
            MACHINE_1_SN,
            MACHINE_1_MODEL,
            now,
            SyncStatus.Synced
        ),
        Machine(
            MACHINE_2,
            CLIENT_1,
            MANUFACTURER_1,
            MACHINE_2_SN,
            MACHINE_2_MODEL,
            now,
            SyncStatus.Synced,
        ),
        Machine(
            MACHINE_3,
            CLIENT_1,
            MANUFACTURER_1,
            MACHINE_3_SN,
            MACHINE_3_MODEL,
            now,
            SyncStatus.Synced
        ),
        Machine(
            MACHINE_4,
            CLIENT_1,
            MANUFACTURER_1,
            MACHINE_4_SN,
            MACHINE_4_MODEL,
            now,
            SyncStatus.Synced
        ),
        Machine(
            MACHINE_5,
            CLIENT_1,
            MANUFACTURER_1,
            MACHINE_5_SN,
            MACHINE_5_MODEL,
            now,
            SyncStatus.Synced
        ),
        Machine(
            MACHINE_6,
            CLIENT_1,
            MANUFACTURER_1,
            MACHINE_6_SN,
            MACHINE_6_MODEL,
            now,
            SyncStatus.Synced
        ),
        Machine(
            MACHINE_7,
            CLIENT_1,
            MANUFACTURER_1,
            MACHINE_7_SN,
            MACHINE_7_MODEL,
            now,
            SyncStatus.Synced
        ),
        Machine(
            MACHINE_8,
            CLIENT_1,
            MANUFACTURER_1,
            MACHINE_8_SN,
            MACHINE_8_MODEL,
            now,
            SyncStatus.Synced
        ),
        Machine(
            MACHINE_9,
            CLIENT_2,
            MANUFACTURER_1,
            MACHINE_9_SN,
            MACHINE_9_MODEL,
            now,
            SyncStatus.Synced
        ),
        Machine(
            MACHINE_10,
            CLIENT_2,
            MANUFACTURER_1,
            MACHINE_10_SN,
            MACHINE_10_MODEL,
            now,
            SyncStatus.Synced
        ),
        Machine(
            MACHINE_11,
            CLIENT_2,
            MANUFACTURER_1,
            MACHINE_11_SN,
            MACHINE_11_MODEL,
            now,
            SyncStatus.Synced
        ),
        Machine(
            MACHINE_12,
            CLIENT_2,
            MANUFACTURER_2,
            MACHINE_12_SN,
            MACHINE_12_MODEL,
            now,
            SyncStatus.Synced
        ),
        Machine(
            MACHINE_13,
            CLIENT_2,
            MANUFACTURER_2,
            MACHINE_13_SN,
            MACHINE_13_MODEL,
            now,
            SyncStatus.Synced
        ),
    )

    // Times as dev.sql has them: reported, then created when the app synced it (hours later for
    // FT-0002 and FT-0103, reported offline), updated at seed time. Status and technician are
    // where the updates below leave them, Room has no trigger to apply them.
    fun incidents(now: Instant) = listOf(
        Incident(
            INCIDENT_1,
            CLIENT_1,
            MACHINE_1,
            MANUFACTURER_1,
            OPERATOR_1.toString(),
            OPERATOR_1_NAME,
            TECHNICIAN_1,
            TECHNICIAN_1_NAME,
            INCIDENT_1_DESC,
            IncidentSeverity.Critical,
            41.1579,
            -8.6291,
            12f,
            now - 2.days,
            now - 2.days + 1.minutes,
            now,
            IncidentStatus.Analysis,
            SyncStatus.Synced
        ),
        Incident(
            INCIDENT_2,
            CLIENT_1,
            MACHINE_2,
            MANUFACTURER_1,
            OPERATOR_2.toString(),
            OPERATOR_2_NAME,
            null,
            null,
            INCIDENT_2_DESC,
            IncidentSeverity.Medium,
            null,
            null,
            null,
            now - 1.days,
            now - 1.days + 3.hours,
            now,
            IncidentStatus.Received,
            SyncStatus.Synced
        ),
        Incident(
            INCIDENT_3,
            CLIENT_1,
            MACHINE_4,
            MANUFACTURER_1,
            OPERATOR_1.toString(),
            OPERATOR_1_NAME,
            TECHNICIAN_2,
            TECHNICIAN_2_NAME,
            INCIDENT_3_DESC,
            IncidentSeverity.Low,
            41.1580,
            -8.6288,
            20f,
            now - 6.days,
            now - 6.days + 2.minutes,
            now,
            IncidentStatus.Resolved,
            SyncStatus.Synced
        ),
        Incident(
            INCIDENT_4,
            CLIENT_1,
            MACHINE_5,
            MANUFACTURER_1,
            OPERATOR_2.toString(),
            OPERATOR_2_NAME,
            TECHNICIAN_3,
            TECHNICIAN_3_NAME,
            INCIDENT_4_DESC,
            IncidentSeverity.Critical,
            41.1577,
            -8.6295,
            8f,
            now - 3.hours,
            now - 3.hours + 1.minutes,
            now,
            IncidentStatus.Received,
            SyncStatus.Synced
        ),
        Incident(
            INCIDENT_5,
            CLIENT_1,
            MACHINE_6,
            MANUFACTURER_1,
            OPERATOR_1.toString(),
            OPERATOR_1_NAME,
            TECHNICIAN_1,
            TECHNICIAN_1_NAME,
            INCIDENT_5_DESC,
            IncidentSeverity.Medium,
            null,
            null,
            null,
            now - 4.days,
            now - 4.days + 5.minutes,
            now,
            IncidentStatus.Analysis,
            SyncStatus.Synced
        ),
        Incident(
            INCIDENT_6,
            CLIENT_1,
            MACHINE_1,
            MANUFACTURER_1,
            OPERATOR_2.toString(),
            OPERATOR_2_NAME,
            TECHNICIAN_1,
            TECHNICIAN_1_NAME,
            INCIDENT_6_DESC,
            IncidentSeverity.Low,
            41.1579,
            -8.6291,
            15f,
            now - 10.days,
            now - 10.days + 1.minutes,
            now,
            IncidentStatus.Resolved,
            SyncStatus.Synced
        ),
        Incident(
            INCIDENT_7,
            CLIENT_1,
            MACHINE_7,
            MANUFACTURER_1,
            OPERATOR_1.toString(),
            OPERATOR_1_NAME,
            null,
            null,
            INCIDENT_7_DESC,
            IncidentSeverity.Critical,
            41.1581,
            -8.6290,
            10f,
            now - 40.minutes,
            now - 40.minutes + 1.minutes,
            now,
            IncidentStatus.Received,
            SyncStatus.Synced
        ),
        Incident(
            INCIDENT_8,
            CLIENT_1,
            MACHINE_8,
            MANUFACTURER_1,
            OPERATOR_2.toString(),
            OPERATOR_2_NAME,
            TECHNICIAN_1,
            TECHNICIAN_1_NAME,
            INCIDENT_8_DESC,
            IncidentSeverity.Medium,
            null,
            null,
            null,
            now - (2.days + 6.hours),
            now - (2.days + 6.hours) + 20.minutes,
            now,
            IncidentStatus.Analysis,
            SyncStatus.Synced
        ),
        Incident(
            INCIDENT_9,
            CLIENT_2,
            MACHINE_9,
            MANUFACTURER_1,
            OPERATOR_3.toString(),
            OPERATOR_3_NAME,
            null,
            null,
            INCIDENT_9_DESC,
            IncidentSeverity.Critical,
            41.5454,
            -8.4265,
            25f,
            now - 5.hours,
            now - 5.hours + 2.minutes,
            now,
            IncidentStatus.Received,
            SyncStatus.Synced
        ),
        Incident(
            INCIDENT_10,
            CLIENT_2,
            MACHINE_10,
            MANUFACTURER_1,
            OPERATOR_3.toString(),
            OPERATOR_3_NAME,
            TECHNICIAN_3,
            TECHNICIAN_3_NAME,
            INCIDENT_10_DESC,
            IncidentSeverity.Medium,
            41.5456,
            -8.4261,
            30f,
            now - 3.days,
            now - 3.days + 1.minutes,
            now,
            IncidentStatus.Analysis,
            SyncStatus.Synced
        ),
        Incident(
            INCIDENT_11,
            CLIENT_2,
            MACHINE_11,
            MANUFACTURER_1,
            OPERATOR_3.toString(),
            OPERATOR_3_NAME,
            TECHNICIAN_2,
            TECHNICIAN_2_NAME,
            INCIDENT_11_DESC,
            IncidentSeverity.Low,
            null,
            null,
            null,
            now - 8.days,
            now - 8.days + 4.hours,
            now,
            IncidentStatus.Resolved,
            SyncStatus.Synced
        ),
        Incident(
            INCIDENT_12,
            CLIENT_2,
            MACHINE_9,
            MANUFACTURER_1,
            OPERATOR_3.toString(),
            OPERATOR_3_NAME,
            TECHNICIAN_1,
            TECHNICIAN_1_NAME,
            INCIDENT_12_DESC,
            IncidentSeverity.Low,
            41.5454,
            -8.4265,
            18f,
            now - 12.days,
            now - 12.days + 1.minutes,
            now,
            IncidentStatus.Resolved,
            SyncStatus.Synced
        ),
        Incident(
            INCIDENT_13,
            CLIENT_2,
            MACHINE_12,
            MANUFACTURER_2,
            OPERATOR_3.toString(),
            OPERATOR_3_NAME,
            TECHNICIAN_4,
            TECHNICIAN_4_NAME,
            INCIDENT_13_DESC,
            IncidentSeverity.Medium,
            41.5455,
            -8.4263,
            15f,
            now - (1.days + 2.hours),
            now - (1.days + 2.hours) + 1.minutes,
            now,
            IncidentStatus.Analysis,
            SyncStatus.Synced
        ),
        Incident(
            INCIDENT_14,
            CLIENT_2,
            MACHINE_13,
            MANUFACTURER_2,
            MANAGER_3.toString(),
            MANAGER_3_NAME,
            null,
            null,
            INCIDENT_14_DESC,
            IncidentSeverity.Low,
            null,
            null,
            null,
            now - 4.hours,
            now - 4.hours + 2.minutes,
            now,
            IncidentStatus.Received,
            SyncStatus.Synced
        ),
    )

    // assigned_to holds the technician's id, as Mappers.kt stores it from the API, and
    // assigned_to_name their name, which the API sends along
    fun incidentUpdates(now: Instant) = listOf(
        // incident 1
        IncidentUpdate(
            UPDATE_1,
            INCIDENT_1,
            ASSISTANCE_1_NAME, // carla
            UPDATE_1_MESSAGE,
            IncidentStatus.Analysis,
            TECHNICIAN_1.toString(),
            TECHNICIAN_1_NAME,
            now - (1.days + 22.hours),
            SyncStatus.Synced
        ),
        IncidentUpdate(
            UPDATE_2,
            INCIDENT_1,
            TECHNICIAN_1_NAME,  // bruno
            UPDATE_2_MESSAGE,
            null,
            null,
            null,
            now - (1.days + 2.hours),
            SyncStatus.Synced
        ),
        // incident 3
        IncidentUpdate(
            UPDATE_3,
            INCIDENT_3,
            ASSISTANCE_1_NAME,
            UPDATE_3_MESSAGE,
            IncidentStatus.Analysis,
            TECHNICIAN_2.toString(),
            TECHNICIAN_2_NAME,
            now - (5.days + 20.hours),
            SyncStatus.Synced
        ),
        IncidentUpdate(
            UPDATE_4,
            INCIDENT_3,
            TECHNICIAN_2_NAME,
            UPDATE_4_MESSAGE,
            null,
            null,
            null,
            now - (4.days + 3.hours),
            SyncStatus.Synced
        ),
        IncidentUpdate(
            UPDATE_5,
            INCIDENT_3,
            ASSISTANCE_1_NAME,
            UPDATE_5_MESSAGE,
            IncidentStatus.Resolved,
            null,
            null,
            now - (4.days + 1.hours),
            SyncStatus.Synced
        ),
        // incident 4, assigned but not started yet
        IncidentUpdate(
            UPDATE_6,
            INCIDENT_4,
            ASSISTANCE_1_NAME,
            UPDATE_6_MESSAGE,
            null,
            TECHNICIAN_3.toString(),
            TECHNICIAN_3_NAME,
            now - 2.hours,
            SyncStatus.Synced
        ),
        IncidentUpdate(
            UPDATE_22,
            INCIDENT_4,
            TECHNICIAN_3_NAME,
            UPDATE_22_MESSAGE,
            null,
            null,
            null,
            now - (1.hours + 30.minutes),
            SyncStatus.Synced
        ),
        // incident 5
        IncidentUpdate(
            UPDATE_7,
            INCIDENT_5,
            OPERATOR_1_NAME,
            UPDATE_7_MESSAGE,
            null,
            null,
            null,
            now - (3.days + 23.hours),
            SyncStatus.Synced
        ),
        IncidentUpdate(
            UPDATE_8,
            INCIDENT_5,
            ASSISTANCE_1_NAME,
            null,
            IncidentStatus.Analysis,
            TECHNICIAN_1.toString(),
            TECHNICIAN_1_NAME,
            now - (3.days + 2.hours),
            SyncStatus.Synced
        ),
        IncidentUpdate(
            UPDATE_9,
            INCIDENT_5,
            TECHNICIAN_1_NAME,
            UPDATE_9_MESSAGE,
            null,
            null,
            null,
            now - (2.days + 5.hours),
            SyncStatus.Synced
        ),
        // incident 6
        IncidentUpdate(
            UPDATE_10,
            INCIDENT_6,
            ASSISTANCE_1_NAME,
            null,
            IncidentStatus.Analysis,
            TECHNICIAN_1.toString(),
            TECHNICIAN_1_NAME,
            now - (9.days + 22.hours),
            SyncStatus.Synced
        ),
        IncidentUpdate(
            UPDATE_11,
            INCIDENT_6,
            TECHNICIAN_1_NAME,
            UPDATE_11_MESSAGE,
            null,
            null,
            null,
            now - (9.days + 3.hours),
            SyncStatus.Synced
        ),
        IncidentUpdate(
            UPDATE_12,
            INCIDENT_6,
            ASSISTANCE_1_NAME,
            UPDATE_12_MESSAGE,
            IncidentStatus.Resolved,
            null,
            null,
            now - (9.days + 1.hours),
            SyncStatus.Synced
        ),
        // incident 8
        IncidentUpdate(
            UPDATE_13,
            INCIDENT_8,
            ASSISTANCE_1_NAME,
            UPDATE_13_MESSAGE,
            IncidentStatus.Analysis,
            TECHNICIAN_1.toString(),
            TECHNICIAN_1_NAME,
            now - (1.days + 20.hours),
            SyncStatus.Synced
        ),
        // incident 10
        IncidentUpdate(
            UPDATE_14,
            INCIDENT_10,
            ASSISTANCE_1_NAME,
            UPDATE_14_MESSAGE,
            IncidentStatus.Analysis,
            TECHNICIAN_3.toString(),
            TECHNICIAN_3_NAME,
            now - (2.days + 20.hours),
            SyncStatus.Synced
        ),
        IncidentUpdate(
            UPDATE_15,
            INCIDENT_10,
            TECHNICIAN_3_NAME,
            UPDATE_15_MESSAGE,
            null,
            null,
            null,
            now - (1.days + 4.hours),
            SyncStatus.Synced
        ),
        // incident 11
        IncidentUpdate(
            UPDATE_16,
            INCIDENT_11,
            ASSISTANCE_1_NAME,
            null,
            IncidentStatus.Analysis,
            TECHNICIAN_2.toString(),
            TECHNICIAN_2_NAME,
            now - (7.days + 18.hours),
            SyncStatus.Synced
        ),
        IncidentUpdate(
            UPDATE_17,
            INCIDENT_11,
            TECHNICIAN_2_NAME,
            UPDATE_17_MESSAGE,
            null,
            null,
            null,
            now - (7.days + 2.hours),
            SyncStatus.Synced
        ),
        IncidentUpdate(
            UPDATE_18,
            INCIDENT_11,
            ASSISTANCE_1_NAME,
            null,
            IncidentStatus.Resolved,
            null,
            null,
            now - (7.days + 1.hours),
            SyncStatus.Synced
        ),
        // incident 12
        IncidentUpdate(
            UPDATE_19,
            INCIDENT_12,
            ASSISTANCE_1_NAME,
            null,
            IncidentStatus.Analysis,
            TECHNICIAN_1.toString(),
            TECHNICIAN_1_NAME,
            now - (11.days + 20.hours),
            SyncStatus.Synced
        ),
        IncidentUpdate(
            UPDATE_20,
            INCIDENT_12,
            TECHNICIAN_1_NAME,
            UPDATE_20_MESSAGE,
            null,
            null,
            null,
            now - (11.days + 3.hours),
            SyncStatus.Synced
        ),
        IncidentUpdate(
            UPDATE_21,
            INCIDENT_12,
            ASSISTANCE_1_NAME,
            null,
            IncidentStatus.Resolved,
            null,
            null,
            now - (11.days + 2.hours),
            SyncStatus.Synced
        ),
        // incident 13, Compressores Atlântico's
        IncidentUpdate(
            UPDATE_23,
            INCIDENT_13,
            ASSISTANCE_2_NAME, // luísa
            UPDATE_23_MESSAGE,
            IncidentStatus.Analysis,
            TECHNICIAN_4.toString(),
            TECHNICIAN_4_NAME,
            now - 1.days,
            SyncStatus.Synced
        ),
        IncidentUpdate(
            UPDATE_24,
            INCIDENT_13,
            TECHNICIAN_4_NAME,
            UPDATE_24_MESSAGE,
            null,
            null,
            null,
            now - 3.hours,
            SyncStatus.Synced
        ),
    )
}