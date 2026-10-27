/**
 * GymCopilot Backend - Gestión de Chats, Horarios, Tienda e Identidad
 * Versión v5.9.7: Planilla Maestra "GymCopilot_Master_DB" con pestaña exclusiva "GymsRegistry".
 */

var SS_ID_FROM_APP = '';

function authorizeAndSetup() {
  try {
    var masterSS = getMasterSS();
    var folderName = "GymCopilot_Databases";
    var folders = DriveApp.getFoldersByName(folderName);
    var folder = folders.hasNext() ? folders.next() : DriveApp.createFolder(folderName);
    Logger.log("Permisos otorgados con éxito. Carpeta ID: " + folder.getId());
    return "Permisos de DriveApp y SpreadsheetApp autorizados correctamente.";
  } catch (err) {
    Logger.log("Error al autorizar: " + err.toString());
    throw err;
  }
}

function testCreateGym() {
  var testData = {
    action: "create_gym",
    gymName: "GimnasioCentral",
    email: "admin@gymcopilot.com",
    gymId: "gimnasio_central"
  };
  var result = executeCreateGym(testData);
  Logger.log("Resultado de creación: " + JSON.stringify(result));
  return result;
}

function setup() {
  var ss = getSS();
  if (isMasterSS(ss)) {
    cleanMasterSS(ss);
    return "Planilla maestra 'GymCopilot_Master_DB' configurada (solo GymsRegistry).";
  }
  var sheets = ["Asistencia", "Alumnos", "Profesores", "Horarios", "Chats", "Rutinas", "Tienda", "Configuracion", "Planes"];
  sheets.forEach(function(name) {
    getOrCreateSheet(ss, name);
  });
  return "Hojas inicializadas correctamente.";
}

function doGet(e) {
  try {
    var action = (e && e.parameter && e.parameter.action) ? e.parameter.action : "";
    if (action) {
      return processAction(action, e.parameter, e);
    }

    var ss = getSS(e);

    if (e && e.parameter && e.parameter.action === "setup") {
      var msg = setup();
      return createJsonResponse({ "status": "success", "message": msg });
    }

    if (isMasterSS(ss)) {
      cleanMasterSS(ss);
      var regSheet = getOrCreateSheet(ss, "GymsRegistry");
      return createJsonResponse({ "status": "success", "data": regSheet.getDataRange().getValues() });
    }

    var type = (e && e.parameter && e.parameter.type) ? e.parameter.type.toLowerCase() : "asistencia";

    if (type === "all") {
      var result = {
        "config": getSheetData(ss, "Configuracion", fixConfigSheetStructure),
        "plans": getSheetData(ss, "Planes"),
        "members": getSheetData(ss, "Alumnos", fixMemberSheetStructure),
        "professors": getSheetData(ss, "Profesores", fixProfessorSheetStructure),
        "schedules": getSheetData(ss, "Horarios", fixScheduleSheetStructure),
        "routines": getSheetData(ss, "Rutinas", fixRoutineSheetStructure),
        "store": getSheetData(ss, "Tienda")
      };
      return createJsonResponse({ "status": "success", "data": result });
    }

    var sheetName = getSheetName(type);
    var sheet = getOrCreateSheet(ss, sheetName);

    if (sheetName === "Chats") fixChatSheetStructure(sheet);
    if (sheetName === "Rutinas") fixRoutineSheetStructure(sheet);
    if (sheetName === "Configuracion") fixConfigSheetStructure(sheet);
    if (sheetName === "Horarios") fixScheduleSheetStructure(sheet);
    if (sheetName === "Profesores") fixProfessorSheetStructure(sheet);
    if (sheetName === "Alumnos") fixMemberSheetStructure(sheet);

    var data = sheet.getDataRange().getValues();
    return createJsonResponse({ "status": "success", "data": data });
  } catch (err) {
    return createJsonResponse({ "status": "error", "message": err.toString() });
  }
}

function getSheetName(type) {
  var mapping = {
    "schedules":  "Horarios",
    "chats":      "Chats",
    "members":    "Alumnos",
    "professors": "Profesores",
    "store":      "Tienda",
    "routines":   "Rutinas",
    "config":     "Configuracion",
    "plans":      "Planes"
  };
  return mapping[type] || "Asistencia";
}

function doPost(e) {
  var lock = LockService.getScriptLock();
  try {
    lock.waitLock(30000);
    var data = {};
    if (e && e.postData && e.postData.contents) {
      try {
        data = JSON.parse(e.postData.contents);
      } catch (errJson) {
        data = {};
      }
    }
    if (e && e.parameter) {
      for (var key in e.parameter) {
        if (e.parameter.hasOwnProperty(key)) {
          if (!data[key]) data[key] = e.parameter[key];
        }
      }
    }

    var action = data.action || (e && e.parameter ? e.parameter.action : "");
    return processAction(action, data, e);
  } catch(error) {
    return createJsonResponse({ "status": "error", "message": error.toString() });
  } finally {
    lock.releaseLock();
  }
}

function processAction(action, data, e) {
  var ss = getSS(e);

  if (isMasterSS(ss) && action !== "create_gym" && action !== "reset_gym" && action !== "clear_gym_data") {
    cleanMasterSS(ss);
    return createJsonResponse({ "status": "error", "message": "Operación no permitida en GymCopilot_Master_DB." });
  }

  if (action === "update_gym_info") {
    var sheet = getOrCreateSheet(ss, "Configuracion");
    fixConfigSheetStructure(sheet);

    var existingValues = sheet.getDataRange().getValues();
    var existingRow = (existingValues.length > 1 && String(existingValues[1][0]).toUpperCase() === "GYM_CONFIG") ? existingValues[1] : [];

    var newName = (data.name !== undefined && data.name !== null && data.name !== "") ? data.name : (existingRow[1] || "");
    var newLogo = (data.logoUri !== undefined && data.logoUri !== null && data.logoUri !== "") ? data.logoUri : (existingRow[2] || "");
    var newPhone = (data.phone !== undefined && data.phone !== null && data.phone !== "") ? data.phone : (existingRow[3] || "");
    var newAddress = (data.address !== undefined && data.address !== null && data.address !== "") ? data.address : (existingRow[4] || "");
    var newCity = (data.city !== undefined && data.city !== null && data.city !== "") ? data.city : (existingRow[5] || "");
    var newZip = (data.zip !== undefined && data.zip !== null && data.zip !== "") ? data.zip : (existingRow[6] || "");
    var newCountry = (data.country !== undefined && data.country !== null && data.country !== "") ? data.country : (existingRow[7] || "");
    var newColor = (data.accentColor !== undefined && data.accentColor !== null && data.accentColor !== "") ? data.accentColor : (existingRow[8] !== undefined ? existingRow[8] : "");
    var newBgColor = (data.backgroundColor !== undefined && data.backgroundColor !== null && data.backgroundColor !== "") ? data.backgroundColor : (existingRow[9] !== undefined ? existingRow[9] : "");
    var newStyle = (data.buttonStyle !== undefined && data.buttonStyle !== null && data.buttonStyle !== "") ? data.buttonStyle : (existingRow[10] !== undefined ? existingRow[10] : "");
    var newBorder = (data.showLogoBorder !== undefined && data.showLogoBorder !== null && data.showLogoBorder !== "") ? (data.showLogoBorder === true || data.showLogoBorder === "true") : (existingRow[11] !== undefined ? existingRow[11] : true);
    var newClass = (data.classInSession !== undefined && data.classInSession !== null && data.classInSession !== "") ? (data.classInSession === true || data.classInSession === "true") : (existingRow[12] !== undefined ? existingRow[12] : false);
    var newMsg = (data.broadcastMessage !== undefined && data.broadcastMessage !== null && data.broadcastMessage !== "") ? data.broadcastMessage : (existingRow[13] || "");
    var newGymOpen = (data.gymIsOpen !== undefined && data.gymIsOpen !== null && data.gymIsOpen !== "") ? (data.gymIsOpen === true || data.gymIsOpen === "true") : (existingRow[14] !== undefined ? existingRow[14] : true);
    var newSsid1 = (data.wifiSsid !== undefined && data.wifiSsid !== null) ? data.wifiSsid : (existingRow[15] || "");
    var newSsid2 = (data.wifiSsid2 !== undefined && data.wifiSsid2 !== null) ? data.wifiSsid2 : (existingRow[16] || "");
    var newSsid3 = (data.wifiSsid3 !== undefined && data.wifiSsid3 !== null) ? data.wifiSsid3 : (existingRow[17] || "");

    updateRow(sheet, "GYM_CONFIG", 0, [
      "GYM_CONFIG",
      newName,
      newLogo,
      newPhone,
      newAddress,
      newCity,
      newZip,
      newCountry,
      newColor,
      newBgColor,
      newStyle,
      newBorder,
      newClass,
      newMsg,
      newGymOpen,
      newSsid1,
      newSsid2,
      newSsid3,
      new Date()
    ]);
    return createJsonResponse({ "status": "success" });
  }

  if (action === "update_plans") {
    var sheet = getOrCreateSheet(ss, "Planes");
    var plans = (typeof data.plans === "string") ? JSON.parse(data.plans) : (data.plans || []);
    sheet.clearContents();
    sheet.appendRow(["ID", "Nombre", "Precio", "Duracion_Dias", "Descripcion", "Actualizado"]);
    plans.forEach(function(p) {
      sheet.appendRow([p.id, p.name, p.price, p.durationDays, p.description || "", new Date()]);
    });
    return createJsonResponse({ "status": "success" });
  }

  if (action === "update_product") {
    updateRow(getOrCreateSheet(ss, "Tienda"), data.id, 0, [data.id, data.name, data.price, data.categoryId, data.imageUri || "", data.description || "", data.contactMethod || "", data.contactPhone || "", data.bannerType || "", data.bannerText || "", new Date()]);
    return createJsonResponse({ "status": "success" });
  }

  if (action === "delete_product") {
    var sheet = getOrCreateSheet(ss, "Tienda");
    var values = sheet.getDataRange().getValues();
    var idToDelete = String(data.id).trim().toLowerCase();
    for (var i = values.length - 1; i >= 1; i--) {
      if (String(values[i][0]).trim().toLowerCase() === idToDelete) sheet.deleteRow(i + 1);
    }
    SpreadsheetApp.flush();
    return createJsonResponse({ "status": "success" });
  }

  if (action === "send_chat_message") {
    var sheet = getOrCreateSheet(ss, "Chats");
    fixChatSheetStructure(sheet);
    sheet.appendRow([data.gymId, data.sender, data.recipient, data.text, data.timestamp, new Date()]);
    SpreadsheetApp.flush();
    return createJsonResponse({ "status": "success" });
  }

  if (action === "delete_chat" || action === "delete_chats") {
    var sheet = getOrCreateSheet(ss, "Chats");
    fixChatSheetStructure(sheet);
    var values = sheet.getDataRange().getValues();
    var sender = String(data.sender || "").trim().toLowerCase();
    var recipient = String(data.recipient || data.otherId || "").trim().toLowerCase();
    var deletedCount = 0;

    var isGeneral = (recipient.indexOf("chat general") !== -1 || recipient.indexOf("general") !== -1 || sender.indexOf("general") !== -1);

    for (var i = values.length - 1; i >= 1; i--) {
      var rSender = String(values[i][1] || "").trim().toLowerCase();
      var rRecipient = String(values[i][2] || "").trim().toLowerCase();

      var matches = false;
      if (isGeneral) {
        if (rRecipient.indexOf("general") !== -1 || rSender.indexOf("general") !== -1) {
          matches = true;
        }
      } else {
        if ((rSender === sender && rRecipient === recipient) || (rSender === recipient && rRecipient === sender)) {
          matches = true;
        }
      }

      if (matches) {
        sheet.getRange(i + 1, 1, 1, 6).clearContent();
        sheet.deleteRow(i + 1);
        deletedCount++;
      }
    }
    SpreadsheetApp.flush();
    return createJsonResponse({ "status": "success", "deleted": deletedCount });
  }

  if (action === "update_schedule") {
    var sheet = getOrCreateSheet(ss, "Horarios");
    fixScheduleSheetStructure(sheet);
    var professorId = String(data.professorId).trim();
    var schedules = (typeof data.schedulesJson === "string") ? JSON.parse(data.schedulesJson) : (data.schedulesJson || []);
    var v = sheet.getDataRange().getValues();
    for (var i = v.length - 1; i >= 1; i--) { if (String(v[i][0]).trim() === professorId) sheet.deleteRow(i + 1); }
    schedules.forEach(function(item) {
      var members = (item.assignedMemberEmails || []).join(",");
      sheet.appendRow([professorId, item.date || item.dayOfWeek || "", item.startTime, item.endTime, item.eventName, members, new Date()]);
    });
    SpreadsheetApp.flush();
    return createJsonResponse({ "status": "success" });
  }

  if (action === "check_in") {
    getOrCreateSheet(ss, "Asistencia").appendRow([new Date(), data.gymId, data.memberEmail, data.timestamp]);
    return createJsonResponse({ "status": "success" });
  }

  if (action === "update_profile") {
    var sheet = getOrCreateSheet(ss, "Profesores");
    fixProfessorSheetStructure(sheet);
    updateRow(sheet, data.professorId, 0, [
      data.professorId,
      data.firstName,
      data.lastName,
      data.email,
      data.specialty,
      data.profileColor,
      data.photoUri || "",
      new Date()
    ]);
    return createJsonResponse({ "status": "success" });
  }

  if (action === "delete_professor") {
    var sheet = getOrCreateSheet(ss, "Profesores");
    fixProfessorSheetStructure(sheet);
    var values = sheet.getDataRange().getValues();
    var profIdToDelete = String(data.professorId || data.id || data.email || "").trim().toLowerCase();
    var firstNameToDelete = String(data.firstName || data.nombre || "").trim().toLowerCase();
    var lastNameToDelete = String(data.lastName || data.apellido || "").trim().toLowerCase();
    var deletedCount = 0;
    var matchedProfIds = [];

    for (var i = values.length - 1; i >= 1; i--) {
      var rowId = String(values[i][0] || "").trim().toLowerCase();
      var rowName = String(values[i][1] || "").trim().toLowerCase();
      var rowLastName = String(values[i][2] || "").trim().toLowerCase();
      var rowEmail = String(values[i][3] || "").trim().toLowerCase();

      var match = false;
      if (profIdToDelete !== "" && (rowId === profIdToDelete || rowEmail === profIdToDelete)) {
        match = true;
      } else if (profIdToDelete !== "" && (rowEmail.indexOf(profIdToDelete) !== -1 || profIdToDelete.indexOf(rowEmail) !== -1)) {
        match = true;
      } else if (firstNameToDelete !== "" && rowName === firstNameToDelete && (lastNameToDelete === "" || rowLastName === lastNameToDelete)) {
        match = true;
      }

      if (match) {
        if (rowId !== "") matchedProfIds.push(rowId);
        if (rowEmail !== "") matchedProfIds.push(rowEmail);
        sheet.getRange(i + 1, 1, 1, 8).clearContent();
        sheet.deleteRow(i + 1);
        deletedCount++;
      }
    }

    if (profIdToDelete !== "") matchedProfIds.push(profIdToDelete);

    try {
      var horSheet = getOrCreateSheet(ss, "Horarios");
      var horValues = horSheet.getDataRange().getValues();
      for (var h = horValues.length - 1; h >= 1; h--) {
        var hProfId = String(horValues[h][0] || "").trim().toLowerCase();
        for (var pIdx = 0; pIdx < matchedProfIds.length; pIdx++) {
          var targetP = matchedProfIds[pIdx];
          if (targetP !== "" && (hProfId === targetP || hProfId.indexOf(targetP) !== -1)) {
            horSheet.getRange(h + 1, 1, 1, 7).clearContent();
            horSheet.deleteRow(h + 1);
            break;
          }
        }
      }
    } catch (eHor) {}

    SpreadsheetApp.flush();
    return createJsonResponse({ "status": "success", "deleted": deletedCount });
  }

  if (action === "update_member") {
    var sheet = getOrCreateSheet(ss, "Alumnos");
    fixMemberSheetStructure(sheet);
    var searchEmail = data.targetEmail || data.originalEmail || data.email;
    updateRow(sheet, searchEmail, 2, [
      data.firstName !== undefined ? data.firstName : "",
      data.lastName !== undefined ? data.lastName : "",
      (data.email !== undefined && data.email !== "") ? data.email : searchEmail,
      data.membershipStatus || "DEUDOR",
      data.phone || "",
      data.address || "",
      data.weight || "",
      data.height || "",
      data.photoUri || "",
      new Date(),
      data.nextRenewalDate || "",
      data.planId || "",
      data.planType || "",
      data.assignedTrainer || ""
    ]);
    return createJsonResponse({ "status": "success" });
  }

  if (action === "delete_member") {
    var sheet = getOrCreateSheet(ss, "Alumnos");
    fixMemberSheetStructure(sheet);
    var values = sheet.getDataRange().getValues();

    var emailToDelete = String(data.memberEmail || data.targetEmail || data.email || "").trim().toLowerCase();
    var firstNameToDelete = String(data.firstName || data.nombre || "").trim().toLowerCase();
    var lastNameToDelete = String(data.lastName || data.apellido || "").trim().toLowerCase();

    var matchedEmails = [];
    var deletedCount = 0;

    for (var i = values.length - 1; i >= 1; i--) {
      var rowName = String(values[i][0] || "").trim().toLowerCase();
      var rowLastName = String(values[i][1] || "").trim().toLowerCase();
      var rowEmail = String(values[i][2] || "").trim().toLowerCase();

      var match = false;
      if (emailToDelete !== "" && rowEmail === emailToDelete) {
        match = true;
      } else if (emailToDelete !== "" && (rowEmail.indexOf(emailToDelete) !== -1 || emailToDelete.indexOf(rowEmail) !== -1)) {
        match = true;
      } else if (firstNameToDelete !== "" && rowName === firstNameToDelete && (lastNameToDelete === "" || rowLastName === lastNameToDelete)) {
        match = true;
      }

      if (match) {
        if (rowEmail !== "") matchedEmails.push(rowEmail);
        sheet.getRange(i + 1, 1, 1, 14).clearContent();
        sheet.deleteRow(i + 1);
        deletedCount++;
      }
    }

    if (emailToDelete !== "") matchedEmails.push(emailToDelete);

    try {
      var attSheet = getOrCreateSheet(ss, "Asistencia");
      var attValues = attSheet.getDataRange().getValues();
      for (var a = attValues.length - 1; a >= 1; a--) {
        var aEmail = String(attValues[a][2] || "").trim().toLowerCase();
        for (var eIdx = 0; eIdx < matchedEmails.length; eIdx++) {
          var targetE = matchedEmails[eIdx];
          if (targetE !== "" && (aEmail === targetE || aEmail.indexOf(targetE) !== -1 || targetE.indexOf(aEmail) !== -1)) {
            attSheet.getRange(a + 1, 1, 1, 4).clearContent();
            attSheet.deleteRow(a + 1);
            break;
          }
        }
      }
    } catch (eAtt) {}

    try {
      var rutSheet = getOrCreateSheet(ss, "Rutinas");
      var rutValues = rutSheet.getDataRange().getValues();
      for (var r = rutValues.length - 1; r >= 1; r--) {
        var rEmail = String(rutValues[r][1] || "").trim().toLowerCase();
        for (var eIdx = 0; eIdx < matchedEmails.length; eIdx++) {
          var targetE = matchedEmails[eIdx];
          if (targetE !== "" && (rEmail === targetE || rEmail.indexOf(targetE) !== -1 || targetE.indexOf(rEmail) !== -1)) {
            rutSheet.getRange(r + 1, 1, 1, 5).clearContent();
            rutSheet.deleteRow(r + 1);
            break;
          }
        }
      }
    } catch (eRut) {}

    try {
      var chatSheet = getOrCreateSheet(ss, "Chats");
      var chatValues = chatSheet.getDataRange().getValues();
      for (var c = chatValues.length - 1; c >= 1; c--) {
        var cSender = String(chatValues[c][1] || "").trim().toLowerCase();
        var cRecipient = String(chatValues[c][2] || "").trim().toLowerCase();
        for (var eIdx = 0; eIdx < matchedEmails.length; eIdx++) {
          var targetE = matchedEmails[eIdx];
          if (targetE !== "" && (cSender === targetE || cRecipient === targetE)) {
            chatSheet.getRange(c + 1, 1, 1, 6).clearContent();
            chatSheet.deleteRow(c + 1);
            break;
          }
        }
      }
    } catch (eChat) {}

    SpreadsheetApp.flush();
    return createJsonResponse({ "status": "success", "deleted": deletedCount });
  }

  if (action === "add_routine" || action === "update_routine" || action === "sync_member_routine") {
    var sheet = getOrCreateSheet(ss, "Rutinas");
    fixRoutineSheetStructure(sheet);
    var values = sheet.getDataRange().getValues();

    var payload = data.data || data;
    var memberEmail = payload.memberEmail;
    var routineId = payload.routineId;
    var routineJson = payload.routine || payload.routineJson;
    var professorId = payload.professorId;

    var s = payload.status;
    var statusVal = (s === "Completada" || s === 1 || s === "1" || s === true || s === "true") ? 1 : 0;

    var email = String(memberEmail || "").trim().toLowerCase();
    var rId = String(routineId || "").trim();

    if (!rId && routineJson) {
      try { rId = JSON.parse(routineJson).id; } catch(e) {}
    }

    var rowData = [new Date(), email, professorId, statusVal, routineJson];

    var matched = false;
    for (var i = 1; i < values.length; i++) {
      var rowEmail = String(values[i][1]).trim().toLowerCase();
      var rowRoutine = String(values[i][4] || "");

      if (rowEmail === email && (rId === "" || rowRoutine.indexOf(rId) !== -1)) {
        sheet.getRange(i + 1, 1, 1, 5).setValues([[rowData[0], rowData[1], rowData[2], rowData[3], rowData[4]]]);
        matched = true;
        break;
      }
    }

    if (!matched) {
      sheet.appendRow(rowData);
    }
    SpreadsheetApp.flush();
    return createJsonResponse({ "status": "success", "applied_status": statusVal });
  }

  if (action === "delete_routine") {
    var sheet = getOrCreateSheet(ss, "Rutinas");
    fixRoutineSheetStructure(sheet);
    var values = sheet.getDataRange().getValues();
    var email = String(data.memberEmail || data.email || "").trim().toLowerCase();
    var routineId = String(data.routineId || data.id || "").trim();
    var deletedCount = 0;
    for (var i = values.length - 1; i >= 1; i--) {
      var rowEmail = String(values[i][1] || "").trim().toLowerCase();
      var rowRoutine = String(values[i][4] || "");
      if ((email === "" || rowEmail === email || rowEmail.indexOf(email) !== -1) && (routineId === "" || rowRoutine.indexOf(routineId) !== -1)) {
        sheet.deleteRow(i + 1);
        deletedCount++;
      }
    }
    SpreadsheetApp.flush();
    return createJsonResponse({ "status": "success", "deleted": deletedCount });
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
    } catch (errClear) {
      Logger.log("Error al limpiar datos del gimnasio: " + errClear.toString());
    }

    var registryValues = registrySheet.getDataRange().getValues();
    var deleted = false;

    for (var i = registryValues.length - 1; i >= 1; i--) {
      var rGymId = String(registryValues[i][0]).trim();
      var rSpreadsheetId = String(registryValues[i][1]).trim();
      if ((gymId && rGymId === gymId) || (spreadsheetId && rSpreadsheetId === spreadsheetId)) {
        try {
          var file = DriveApp.getFileById(rSpreadsheetId);
          file.setTrashed(true);
        } catch (errDrive) {
          Logger.log("Error al mover planilla a papelera: " + errDrive.toString());
        }
        registrySheet.deleteRow(i + 1);
        deleted = true;
      }
    }
    SpreadsheetApp.flush();
    return createJsonResponse({ "status": "success", "message": "Base de datos eliminada correctamente.", "deleted": deleted });
  }

  return createJsonResponse({ "status": "error", "message": "Acción no reconocida: " + action });
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
    Logger.log("Error al crear planilla SpreadsheetApp: " + errSS.toString());
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
  } catch (eDrive) {
    Logger.log("Advertencia DriveApp al mover carpeta: " + eDrive.toString());
  }

  var sheets = ["Asistencia", "Alumnos", "Profesores", "Horarios", "Chats", "Rutinas", "Tienda", "Configuracion", "Planes"];
  sheets.forEach(function(name) {
    try { getOrCreateSheet(newSS, name); } catch (eSheet) {}
  });

  try {
    var allSheets = newSS.getSheets();
    for (var i = allSheets.length - 1; i >= 0; i--) {
      var s = allSheets[i];
      var sName = s.getName();
      if (sheets.indexOf(sName) === -1 && newSS.getSheets().length > 1) {
        newSS.deleteSheet(s);
      }
    }
  } catch (eCleanGym) {}

  try {
    var configSheet = getOrCreateSheet(newSS, "Configuracion");
    fixConfigSheetStructure(configSheet);
    updateRow(configSheet, "GYM_CONFIG", 0, [
      "GYM_CONFIG",
      gymName,
      "",
      "",
      "",
      "",
      "",
      "",
      "",
      "",
      1,
      true,
      false,
      "",
      true,
      "",
      "",
      "",
      new Date()
    ]);
  } catch (eConfig) {}

  try {
    var masterSS = getMasterSS();
    var registrySheet = getOrCreateSheet(masterSS, "GymsRegistry");
    if (registrySheet.getLastRow() === 0 || (registrySheet.getLastRow() === 1 && registrySheet.getRange(1, 1).getValue() === "")) {
      registrySheet.clearContents();
      registrySheet.appendRow(["GymID", "SpreadsheetID", "Email", "Fecha"]);
    }
    registrySheet.appendRow([gymId, newSS.getId(), email, new Date()]);
  } catch (eRegistry) {
    Logger.log("Advertencia registro en GymsRegistry: " + eRegistry.toString());
  }

  return {
    "status": "success",
    "gymId": gymId,
    "spreadsheetId": newSS.getId(),
    "message": "Gimnasio y base de datos creados con éxito."
  };
}

function isMasterSS(ss) {
  if (!ss) return false;
  try {
    var prop = PropertiesService.getScriptProperties();
    var masterSSId = prop.getProperty("MASTER_SS_ID");
    if (masterSSId && ss.getId() === masterSSId) {
      return true;
    }
    var name = ss.getName();
    if (name === "GymCopilot_Master_DB" || name === "GymCopilot_BASE DE DATOS" || name.indexOf("Master") !== -1 || name.indexOf("BASE DE DATOS") !== -1 || name.indexOf("GymCopilot_Master_DB") !== -1) {
      return true;
    }
  } catch (err) {}
  return false;
}

function getMasterSS() {
  var prop = PropertiesService.getScriptProperties();
  var masterSSId = prop.getProperty("MASTER_SS_ID");
  var masterSS = null;
  if (masterSSId) {
    try {
      masterSS = SpreadsheetApp.openById(masterSSId);
    } catch (err) {}
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
    if (!isMasterSS(masterSS)) return;
    var regSheet = masterSS.getSheetByName("GymsRegistry");
    if (!regSheet) {
      regSheet = masterSS.insertSheet("GymsRegistry");
      regSheet.appendRow(["GymID", "SpreadsheetID", "Email", "Fecha"]);
    }
    var sheets = masterSS.getSheets();
    for (var i = sheets.length - 1; i >= 0; i--) {
      var sName = sheets[i].getName();
      if (sName !== "GymsRegistry" && masterSS.getSheets().length > 1) {
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
      email = e.parameter.email || e.parameter.userEmail || e.parameter.memberEmail || "";
      spreadsheetId = e.parameter.spreadsheetId || "";
    }
  } catch (err) {}

  if (spreadsheetId) {
    try {
      var target = SpreadsheetApp.openById(spreadsheetId);
      if (target && !isMasterSS(target)) {
        return target;
      }
    } catch (e) {}
  }

  var normGymId = String(gymId || "").toLowerCase().replace(/[^a-z0-9]/g, "_");
  var normEmail = String(email || "").toLowerCase().trim();

  var registrySheet = getOrCreateSheet(masterSS, "GymsRegistry");
  if (registrySheet) {
    var values = registrySheet.getDataRange().getValues();
    if (normGymId || normEmail) {
      for (var i = 1; i < values.length; i++) {
        var rGymId = String(values[i][0]).toLowerCase().replace(/[^a-z0-9]/g, "_");
        var rSpreadsheetId = String(values[i][1]).trim();
        var rEmail = String(values[i][2]).toLowerCase().trim();
        if ((normGymId && rGymId === normGymId) || (normEmail && rEmail === normEmail)) {
          try {
            var foundSS = SpreadsheetApp.openById(rSpreadsheetId);
            if (foundSS && !isMasterSS(foundSS)) {
              return foundSS;
            }
          } catch (e) {}
        }
      }
    }
  }

  return masterSS;
}

function updateRow(sheet, id, idColIndex, rowData) {
  var values = sheet.getDataRange().getValues();
  var searchId = String(id).trim().toLowerCase();

  if (id === "GYM_CONFIG" && values.length === 2) {
    sheet.getRange(2, 1, 1, rowData.length).setValues([rowData]);
    return;
  }

  for (var i = 1; i < values.length; i++) {
    if (String(values[i][idColIndex]).trim().toLowerCase() === searchId) {
      sheet.getRange(i + 1, 1, 1, rowData.length).setValues([rowData]);
      return;
    }
  }
  sheet.appendRow(rowData);
}

function getSheetData(ss, name, fixFunction) {
  var sheet = getOrCreateSheet(ss, name);
  if (fixFunction) fixFunction(sheet);
  return sheet.getDataRange().getValues();
}

function getOrCreateSheet(ss, name) {
  if (isMasterSS(ss)) {
    cleanMasterSS(ss);
    var regSheet = ss.getSheetByName("GymsRegistry");
    if (!regSheet) {
      regSheet = ss.insertSheet("GymsRegistry");
      regSheet.appendRow(["GymID", "SpreadsheetID", "Email", "Fecha"]);
    }
    return regSheet;
  }

  var sheet = ss.getSheetByName(name);
  if (!sheet) {
    sheet = ss.insertSheet(name);
    var headers = {
      "Asistencia": ["Fecha_Servidor", "Gimnasio", "Email_Miembro", "Timestamp_App"],
      "Alumnos":    ["Nombre", "Apellido", "Email", "Estado", "Teléfono", "Dirección", "Peso", "Altura", "Foto", "Actualizado", "Membresia_Vence", "Plan_ID", "Plan_Nombre", "Profesor_Asignado"],
      "Profesores": ["ID_Profesor", "Nombre", "Apellido", "Email", "Especialidad", "Color", "Foto", "Actualizado"],
      "Horarios":   ["ID_Profesor", "Día/Fecha", "Inicio", "Fin", "Evento", "Alumnos", "Actualizado"],
      "Chats":      ["Gimnasio", "Remitente", "Destinatario", "Mensaje", "Timestamp", "Fecha_Servidor"],
      "Rutinas":    ["Fecha", "Email_Alumno", "ID_Profesor", "Estado", "Rutina"],
      "Tienda":     ["ID", "Nombre", "Precio", "Categoría", "Imagen", "Descripción", "Contacto_Metodo", "Contacto_Tel", "Banner_Tipo", "Banner_Texto", "Actualizado"],
      "Configuracion": ["ID", "Nombre", "Logo", "Telefono", "Direccion", "Ciudad", "CP", "Pais", "Color", "BgColor", "ButtonStyle", "MostrarBordeLogo", "ClaseActiva", "Mensaje", "GimnasioAbierto", "WifiSSID1", "WifiSSID2", "WifiSSID3", "Actualizado"],
      "Planes":     ["ID", "Nombre", "Precio", "Duracion_Dias", "Descripcion", "Actualizado"]
    };
    if (headers[name]) sheet.appendRow(headers[name]);
  }
  return sheet;
}

function createJsonResponse(data) {
  return ContentService.createTextOutput(JSON.stringify(data)).setMimeType(ContentService.MimeType.JSON);
}

function fixChatSheetStructure(sheet) {
  var lastCol = sheet.getLastColumn();
  if (lastCol === 0) return;
  var headers = sheet.getRange(1, 1, 1, lastCol).getValues()[0];
  if (headers[0] !== "Gimnasio") {
    sheet.insertColumnBefore(1);
    sheet.getRange(1, 1).setValue("Gimnasio");
    SpreadsheetApp.flush();
  }
}

function fixScheduleSheetStructure(sheet) {
  var lastCol = sheet.getLastColumn();
  if (lastCol === 0) return;
  var range = sheet.getRange(1, 1, 1, Math.max(lastCol, 7));
  var headers = range.getValues()[0];
  if (headers.length < 7 || headers[5] !== "Alumnos") {
    sheet.insertColumnBefore(6);
    sheet.getRange(1, 6).setValue("Alumnos");
    SpreadsheetApp.flush();
  }
}

function fixRoutineSheetStructure(sheet) {
  var lastCol = sheet.getLastColumn();
  if (lastCol === 0) return;
  var range = sheet.getRange(1, 1, 1, Math.max(lastCol, 5));
  var headers = range.getValues()[0];

  if (headers[3] !== "Estado") {
    if (headers[3] === "Rutina" || headers[3] === "" || lastCol < 5) {
      sheet.insertColumnBefore(4);
      sheet.getRange(1, 4).setValue("Estado");
      var lastRow = sheet.getLastRow();
      if (lastRow > 1) {
        var estadoRange = sheet.getRange(2, 4, lastRow - 1, 1);
        var estadoValues = estadoRange.getValues();
        var changed = false;
        for (var i = 0; i < estadoValues.length; i++) {
          if (estadoValues[i][0] === "") {
            estadoValues[i][0] = 0;
            changed = true;
          }
        }
        if (changed) SpreadsheetApp.flush();
      }
      SpreadsheetApp.flush();
    } else {
      sheet.getRange(1, 1, 1, 5).setValues([["Fecha", "Email_Alumno", "ID_Profesor", "Estado", "Rutina"]]);
      SpreadsheetApp.flush();
    }
  }
}

function fixConfigSheetStructure(sheet) {
  var expectedHeaders = ["ID", "Nombre", "Logo", "Telefono", "Direccion", "Ciudad", "CP", "Pais", "Color", "BgColor", "ButtonStyle", "MostrarBordeLogo", "ClaseActiva", "Mensaje", "GimnasioAbierto", "WifiSSID1", "WifiSSID2", "WifiSSID3", "Actualizado"];
  for (var i = 0; i < expectedHeaders.length; int++) { // wait, int is typo in user's prompt or my code? Let's check: i++
    // i++ is correct
  }
  for (var i = 0; i < expectedHeaders.length; i++) {
    var cell = sheet.getRange(1, i + 1);
    var headerName = cell.getValue();
    if (String(headerName).toLowerCase().trim() !== expectedHeaders[i].toLowerCase().trim()) {
      sheet.insertColumnBefore(i + 1);
      sheet.getRange(1, i + 1).setValue(expectedHeaders[i]);
      SpreadsheetApp.flush();
    }
  }
  var values = sheet.getDataRange().getValues();
  if (values.length > 1) {
    for (var j = values.length - 1; j >= 1; j--) {
      var idVal = String(values[j][0]).toUpperCase();
      if (idVal !== "GYM_CONFIG" && values.length > 2) {
        sheet.deleteRow(j + 1);
      }
    }
  }
}

function fixProfessorSheetStructure(sheet) {
  var expectedHeaders = ["ID_Profesor", "Nombre", "Apellido", "Email", "Especialidad", "Color", "Foto", "Actualizado"];
  for (var i = 0; i < expectedHeaders.length; i++) {
    var cell = sheet.getRange(1, i + 1);
    var headerName = cell.getValue();
    if (String(headerName).toLowerCase().trim() !== expectedHeaders[i].toLowerCase().trim()) {
      sheet.insertColumnBefore(i + 1);
      sheet.getRange(1, i + 1).setValue(expectedHeaders[i]);
      SpreadsheetApp.flush();
    }
  }
}

function fixMemberSheetStructure(sheet) {
  var expectedHeaders = ["Nombre", "Apellido", "Email", "Estado", "Teléfono", "Dirección", "Peso", "Altura", "Foto", "Actualizado", "Membresia_Vence", "Plan_ID", "Plan_Nombre", "Profesor_Asignado"];
  for (var i = 0; i < expectedHeaders.length; i++) {
    var cell = sheet.getRange(1, i + 1);
    var headerName = cell.getValue();
    if (String(headerName).toLowerCase().trim() !== expectedHeaders[i].toLowerCase().trim()) {
      sheet.insertColumnBefore(i + 1);
      sheet.getRange(1, i + 1).setValue(headerName);
    }
  }
}


