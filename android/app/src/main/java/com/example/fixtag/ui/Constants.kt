package com.example.fixtag.ui

const val INCIDENT_ID_KEY: String = "incidentId"
const val INCIDENT_UPDATE_ID_KEY: String = "incidentUpdateId"
const val ATTACHMENT_ID_KEY: String = "attachmentId"
const val MACHINE_ID_KEY: String = "machineId"
const val TECHNICIAN_ID_KEY: String = "technicianId"
const val ORGANIZATION_ID_KEY: String = "organizationId"

const val SELECT_ORGANIZATION_REQUEST_KEY: String = "selectOrganizationId"
const val SELECT_MACHINE_REQUEST_KEY: String = "selectMachineId"

// Handles for savedStateHandle usage

// Organization
const val ORGANIZATION_ID_HANDLE: String = "organizationId"
const val ORGANIZATION_NAME_HANDLE: String = "name"

// Machine
const val MACHINE_ID_HANDLE: String = "machineId"
const val MACHINE_MODEL_HANDLE: String = "model"
const val MACHINE_SERIAL_NUMBER_HANDLE: String = "serialNumber"

// Incident
const val INCIDENT_ID_HANDLE: String = "incidentId"
const val PENDING_PHOTO_HANDLE: String = "pendingPhotoPath"
const val PHOTOS_HANDLE: String = "takenPhotoPaths"
const val PENDING_AUDIO_HANDLE: String = "pendingPhotoPath"
const val AUDIOS_HANDLE: String = "recordedAudioPaths"
const val PENDING_VIDEO_HANDLE: String = "pendingVideoPath"
const val VIDEOS_HANDLE: String = "capturedVideoPaths"
const val GEO_LATITUDE_HANDLE: String = "geoLatitude"
const val GEO_LONGITUDE_HANDLE: String = "geoLongitude"
const val GEO_ACCURACY_HANDLE: String = "geoAccuracyM"
