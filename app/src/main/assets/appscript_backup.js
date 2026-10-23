function doGet(e) {
  return handleRequest(e);
}

function doPost(e) {
  return handleRequest(e);
}

function handleRequest(e) {
  var ss = getSS(e);
  var action = (e && e.parameter && e.parameter.action) ? e.parameter.action : "";
  var data = {};

  if (e && e.postData && e.postData.contents) {
    try {
      data = JSON.parse(e.postData.contents);
      if (!action && data.action) action = data.action;
    } catch (err) {}
  }

  if (action === "check_in") {
    var sheet = getOrCreateSheet(ss, "Asistencia");
    fixAttendanceSheetStructure(sheet);
    var dateStr = formatDate(new Date());
    var email = data.memberEmail || data.email || (e && e.parameter ? e.parameter.memberEmail : "") || "";
    var gymId = data.gymId || (e && e.parameter ? e.parameter.gymId : "") || "";
    sheet.appendRow([new Date(), gymId, email, data.timestamp || new Date().getTime()]);
    SpreadsheetApp.flush();
    return createJsonResponse({ "status": "success", "message": "Asistencia registrada" });
  }

  if (action === "send_chat_message") {
    var sheet = getOrCreateSheet(ss, "Chats");
    fixChatSheetStructure(sheet);
    var gymId = data.gymId || (e && e.parameter ? e.parameter.gymId : "") || "";
    var sender = data.sender || "";
    var recipient = data.recipient || "";
    var text = data.text || "";
    var timestamp = data.timestamp || new Date().getTime();
    sheet.appendRow([gymId, sender, recipient, text, timestamp, new Date()]);
    SpreadsheetApp.flush();
    return createJsonResponse({ "status": "success", "message": "Mensaje enviado" });
  }

  if (action === "update_member") {
    var sheet = getOrCreateSheet(ss, "Alumnos");
    fixMemberSheetStructure(sheet);
    var email = String(data.email || "").trim().toLowerCase();
    var rowData = [
      data.firstName || "",
      data.lastName || "",
      email,
      data.membershipStatus || "DEUDOR",
      data.phone || "",
      data.address || "",
      data.weight || "",
      data.height || "",
      data.photoUri || "",
      new Date(),
      data.planType || "",
      data.assignedTrainer || "",
      data.notes || ""
    ];
    updateRow(sheet, email, 2, rowData);
    SpreadsheetApp.flush();
    return createJsonResponse({ "status": "success", "message": "Alumno actualizado" });
  }

  if (action === "update_profile") {
    var sheet = getOrCreateSheet(ss, "Profesores");
    fixProfessorSheetStructure(sheet);
    var email = String(data.email || "").trim().toLowerCase();
    var rowData = [
      data.professorId || "",
      data.firstName || "",
      data.lastName || "",
      email,
      data.specialty || "",
      data.phone || "",
      data.address || "",
      data.photoUri || "",
      data.profileColor || "",
      new Date()
    ];
    updateRow(sheet, email, 3, rowData);
    SpreadsheetApp.flush();
    return createJsonResponse({ "status": "success", "message": "Profesor actualizado" });
  }

  if (action === "update_schedule") {
    var sheet = getOrCreateSheet(ss, "Horarios");
    fixScheduleSheetStructure(sheet);
    var profId = String(data.professorId || "").trim();
    var rowData = [profId, data.schedulesJson || "[]", new Date()];
    updateRow(sheet, profId, 0, rowData);
    SpreadsheetApp.flush();
    return createJsonResponse({ "status": "success", "message": "Horario actualizado" });
  }

  if (action === "update_plans") {
    var sheet = getOrCreateSheet(ss, "Planes");
    fixPlansSheetStructure(sheet);
    if (data.plans && Array.isArray(data.plans)) {
      sheet.clearContents();
      sheet.appendRow(["ID", "Nombre", "Precio", "Dias", "Descripcion", "Fecha"]);
      data.plans.forEach(function(p) {
        sheet.appendRow([p.id || "", p.name || "", p.price || "", p.days || "", p.description || "", new Date()]);
      });
    }
    SpreadsheetApp.flush();
    return createJsonResponse({ "status": "success", "message": "Planes actualizados" });
  }

  if (action === "update_routine") {
    var sheet = getOrCreateSheet(ss, "Rutinas");
    fixRoutineSheetStructure(sheet);
    var email = String(data.memberEmail || data.email || "").trim().toLowerCase();
    var rId = String(data.routineId || "").trim();
    var statusVal = data.status || "ACTIVA";
    var rowData = [data.professorId || "", email, data.routine || "", statusVal, rId, new Date()];

    var values = sheet.getDataRange().getValues();
    var matched = false;
    for (var i = 1; i < values.length; i++) {
      var rowEmail = String(values[i][1]).trim().toLowerCase();
      var rowRoutine = String(values[i][4] || "");
      if (rowEmail === email && (rId === "" || rowRoutine.indexOf(rId) !== -1)) {
        sheet.getRange(i + 1, 1, 1, rowData.length).setValues([rowData]);
        matched = true;
        break;
      }
    }
    if (!matched) sheet.appendRow(rowData);
    SpreadsheetApp.flush();
    return createJsonResponse({ "status": "success", "message": "Rutina actualizada" });
  }

  if (action === "create_gym") {
    var result = executeCreateGym(data);
    return createJsonResponse(result);
  }

  if (action === "reset_gym" || action === "clear_gym_data") {
    var gymId = data.gymId || (e && e.parameter ? e.parameter.gymId : "") || "";
    var spreadsheetId = data.spreadsheetId || (e && e.parameter ? e.parameter.spreadsheetId : "") || "";
    var masterSS = getMasterSS();
    var registrySheet = getOrCreateSheet(masterSS, "GymsRegistry");

    try {
      var gymSS = getSS(e);
      if (gymSS && gymSS.getId() !== masterSS.getId()) {
        var sheets = gymSS.getSheets();
        sheets.forEach(function(s) {
          if (s.getName() !== "GymsRegistry") {
            s.clearContents();
          }
        });
        SpreadsheetApp.flush();
      }
    } catch (errClear) {}

    var registryValues = registrySheet.getDataRange().getValues();
    var deleted = false;
    for (var i = registryValues.length - 1; i >= 1; i--) {
      var rGymId = String(registryValues[i][0]).trim();
      var rSpreadsheetId = String(registryValues[i][1]).trim();
      if ((gymId && rGymId === gymId) || (spreadsheetId && rSpreadsheetId === spreadsheetId)) {
        try {
          var file = DriveApp.getFileById(rSpreadsheetId);
          file.setTrashed(true);
        } catch (errDrive) {}
        registrySheet.deleteRow(i + 1);
        deleted = true;
      }
    }
    SpreadsheetApp.flush();
    return createJsonResponse({ "status": "success", "message": "Base de datos eliminada correctamente.", "deleted": deleted });
  }

  // MegaSync GET / POST
  var megaData = getMegaSyncData(ss);
  return createJsonResponse({ "status": "success", "data": megaData });
}

function getMasterSS() {
  var prop = PropertiesService.getScriptProperties();
  var masterSSId = prop.getProperty("MASTER_SS_ID");
  var masterSS = null;
  if (masterSSId) {
    try { masterSS = SpreadsheetApp.openById(masterSSId); } catch (err) {}
  }

  if (!masterSS) {
    try {
      masterSS = SpreadsheetApp.create("GymCopilot_Master_DB");
      prop.setProperty("MASTER_SS_ID", masterSS.getId());
    } catch (err) {
      throw new Error("No se pudo crear/abrir la planilla maestra: " + err.toString());
    }
  }

  cleanMasterSS(masterSS);
  return masterSS;
}

function cleanMasterSS(masterSS) {
  try {
    var regSheet = masterSS.getSheetByName("GymsRegistry");
    if (!regSheet) {
      regSheet = masterSS.insertSheet("GymsRegistry");
      regSheet.appendRow(["GymID", "SpreadsheetID", "Email", "Fecha"]);
    }
    var sheets = masterSS.getSheets();
    for (var i = sheets.length - 1; i >= 0; i--) {
      var sName = sheets[i].getName();
      if (sName !== "GymsRegistry" && sheets.length > 1) {
        masterSS.deleteSheet(sheets[i]);
      }
    }
  } catch (eClean) {}
}

function getSS(e) {
  var masterSS = getMasterSS();
  cleanMasterSS(masterSS);

  var gymId = "";
  var email = "";
  var spreadsheetId = "";

  try {
    if (e && e.postData && e.postData.contents) {
      var body = JSON.parse(e.postData.contents);
      gymId = body.gymId || (e.parameter ? e.parameter.gymId : "") || "";
      email = body.email || body.memberEmail || body.userEmail || (e.parameter ? e.parameter.email : "") || "";
      spreadsheetId = body.spreadsheetId || (e.parameter ? e.parameter.spreadsheetId : "") || "";
    } else if (e && e.parameter) {
      gymId = e.parameter.gymId || "";
      email = e.parameter.email || e.parameter.userEmail || "";
      spreadsheetId = e.parameter.spreadsheetId || "";
    }
  } catch (err) {}

  if (spreadsheetId) {
    try {
      var target = SpreadsheetApp.openById(spreadsheetId);
      if (target && target.getName().indexOf("Master") === -1 && target.getName() !== "GymCopilot_Master_DB") {
        return target;
      }
    } catch (e) {}
  }

  var registrySheet = masterSS.getSheetByName("GymsRegistry");
  if (!registrySheet) {
    registrySheet = masterSS.insertSheet("GymsRegistry");
    registrySheet.appendRow(["GymID", "SpreadsheetID", "Email", "Fecha"]);
  }

  var values = registrySheet.getDataRange().getValues();
  var normGymId = String(gymId).toLowerCase().replace(/[^a-z0-9]/g, "");
  var normEmail = String(email).toLowerCase().trim();

  // 1. Coincidencia directa por GymID o Email registrado
  if (normGymId !== "" || normEmail !== "") {
    for (var i = 1; i < values.length; i++) {
      var rGymId = String(values[i][0]).toLowerCase().replace(/[^a-z0-9]/g, "");
      var rSpreadsheetId = String(values[i][1]).trim();
      var rEmail = String(values[i][2]).trim().toLowerCase();

      if ((normGymId !== "" && rGymId === normGymId) || (normEmail !== "" && rEmail === normEmail)) {
        try {
          var foundSS = SpreadsheetApp.openById(rSpreadsheetId);
          if (foundSS && foundSS.getName().indexOf("Master") === -1 && foundSS.getName() !== "GymCopilot_Master_DB") {
            return foundSS;
          }
        } catch (e) {}
      }
    }
  }

  // 2. Si no hubo coincidencia (o es una petición inicial), retornar la planilla del último gimnasio activo en GymsRegistry
  if (values.length > 1) {
    for (var j = values.length - 1; j >= 1; j--) {
      var lastSsId = String(values[j][1]).trim();
      if (lastSsId) {
        try {
          var lastSS = SpreadsheetApp.openById(lastSsId);
          if (lastSS && lastSS.getName().indexOf("Master") === -1 && lastSS.getName() !== "GymCopilot_Master_DB") {
            return lastSS;
          }
        } catch (e) {}
      }
    }
  }

  return masterSS;
}

function executeCreateGym(data) {
  var gymName = data.gymName || "Mi Gimnasio";
  var email = data.email || "";
  var gymId = data.gymId || gymName.toLowerCase().replace(/[^a-z0-9]/g, "_");

  var dbName = "GymCopilot_DB_" + gymName + "_" + gymId;
  var newSS = null;
  try {
    newSS = SpreadsheetApp.create(dbName);
  } catch (errSS) {
    throw errSS;
  }

  try {
    var folderName = "GymCopilot_Databases";
    var folders = DriveApp.getFoldersByName(folderName);
    var folder = folders.hasNext() ? folders.next() : DriveApp.createFolder(folderName);

    var file = DriveApp.getFileById(newSS.getId());
    try {
      file.moveTo(folder);
    } catch (eMove) {
      folder.addFile(file);
      try { DriveApp.getRootFolder().removeFile(file); } catch (err) {}
    }
  } catch (eDrive) {}

  var sheets = ["Asistencia", "Alumnos", "Profesores", "Horarios", "Chats", "Rutinas", "Tienda", "Configuracion", "Planes"];
  sheets.forEach(function(name) {
    try { getOrCreateSheet(newSS, name); } catch (eSheet) {}
  });

  try {
    var masterSS = getMasterSS();
    var registrySheet = masterSS.getSheetByName("GymsRegistry");
    if (!registrySheet) {
      registrySheet = masterSS.insertSheet("GymsRegistry");
    }
    if (registrySheet.getLastRow() === 0 || (registrySheet.getLastRow() === 1 && registrySheet.getRange(1, 1).getValue() === "")) {
      registrySheet.clearContents();
      registrySheet.appendRow(["GymID", "SpreadsheetID", "Email", "Fecha"]);
    }
    registrySheet.appendRow([gymId, newSS.getId(), email, new Date()]);
  } catch (eRegistry) {}

  return {
    "status": "success",
    "gymId": gymId,
    "spreadsheetId": newSS.getId(),
    "message": "Gimnasio y base de datos creados con éxito."
  };
}

function getOrCreateSheet(ss, name) {
  var ssName = ss.getName();
  if (ssName === "GymCopilot_Master_DB" || ssName.indexOf("Master") !== -1) {
    if (name !== "GymsRegistry") {
      var regSheet = ss.getSheetByName("GymsRegistry");
      if (!regSheet) {
        regSheet = ss.insertSheet("GymsRegistry");
        regSheet.appendRow(["GymID", "SpreadsheetID", "Email", "Fecha"]);
      }
      return regSheet;
    }
  }

  var sheet = ss.getSheetByName(name);
  if (!sheet) {
    sheet = ss.insertSheet(name);
  }
  return sheet;
}

function fixAttendanceSheetStructure(sheet) {
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(["Fecha_Servidor", "Gimnasio", "Email_Miembro", "Timestamp_App"]);
  }
}

function fixChatSheetStructure(sheet) {
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(["Gimnasio", "Remitente", "Destinatario", "Mensaje", "Timestamp", "Fecha_Servidor"]);
  }
}

function fixMemberSheetStructure(sheet) {
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(["Nombre", "Apellido", "Email", "Estado", "Teléfono", "Dirección", "Peso", "Altura", "Foto", "Actualización", "Plan", "Profesor", "Notas"]);
  }
}

function fixProfessorSheetStructure(sheet) {
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(["ID", "Nombre", "Apellido", "Email", "Especialidad", "Teléfono", "Dirección", "Foto", "Color", "Fecha"]);
  }
}

function fixScheduleSheetStructure(sheet) {
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(["ProfessorID", "SchedulesJson", "Fecha"]);
  }
}

function fixPlansSheetStructure(sheet) {
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(["ID", "Nombre", "Precio", "Dias", "Descripcion", "Fecha"]);
  }
}

function fixRoutineSheetStructure(sheet) {
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(["ProfessorID", "MemberEmail", "Routine", "Status", "RoutineID", "Fecha"]);
  }
}

function fixConfigSheetStructure(sheet) {
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(["ConfigID", "GymName", "LogoUri", "Phone", "Address", "City", "Zip", "Country", "AccentColor", "BgColor", "ButtonStyle", "ShowLogoBorder", "ClassInSession", "BroadcastMsg", "GymIsOpen", "Wifi1", "Wifi2", "Wifi3", "Fecha"]);
  }
}

function formatDate(date) {
  return Utilities.formatDate(date, Session.getScriptTimeZone(), "yyyy-MM-dd HH:mm:ss");
}

function createJsonResponse(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}

function getMegaSyncData(ss) {
  if (ss.getName().indexOf("Master") !== -1 || ss.getName() === "GymCopilot_Master_DB") {
    return {
      "asistencia": [], "alumnos": [], "profesores": [], "horarios": [], "chats": [], "rutinas": [], "planes": []
    };
  }

  return {
    "asistencia": getSheetData(ss, "Asistencia", fixAttendanceSheetStructure),
    "alumnos": getSheetData(ss, "Alumnos", fixMemberSheetStructure),
    "profesores": getSheetData(ss, "Profesores", fixProfessorSheetStructure),
    "horarios": getSheetData(ss, "Horarios", fixScheduleSheetStructure),
    "chats": getSheetData(ss, "Chats", fixChatSheetStructure),
    "rutinas": getSheetData(ss, "Rutinas", fixRoutineSheetStructure),
    "planes": getSheetData(ss, "Planes", fixPlansSheetStructure)
  };
}

function getSheetData(ss, name, fixFunction) {
  var sheet = getOrCreateSheet(ss, name);
  if (fixFunction) fixFunction(sheet);
  return sheet.getDataRange().getValues();
}

function updateRow(sheet, id, idColIndex, rowData) {
  var values = sheet.getDataRange().getValues();
  var searchId = String(id).trim().toLowerCase();

  for (var i = 1; i < values.length; i++) {
    if (String(values[i][idColIndex]).trim().toLowerCase() === searchId) {
      sheet.getRange(i + 1, 1, 1, rowData.length).setValues([rowData]);
      return;
    }
  }
  sheet.appendRow(rowData);
}
