// Wrappers round AJAX calls to simplify *most* of this stuff

/** How to retrieve text asynchronously. */
function getText(u, done) {
	$.ajax({
		type : "GET",
		url : u,
		async : true,
		cache : false,
		accept : "text/plain",
		dataType : "text",
		success : done
	});
}
/** How to retrieve JSON asynchronously. */
function getJSON(u, done) {
	$.ajax({
		type : "GET",
		url : u,
		async : true,
		cache : false,
		dataType : "json",
		accept : "application/json",
		success : done
	});
}
/** How to send a PUT of text asynchronously. */
function putText(u, val, done) {
	$.ajax({
		type : "PUT",
		url : u,
		async : false,
		cache : false,
		data : val,
		contentType : "text/plain",
		dataType : "text",
		processData : false,
		success : done,
		error : function(jqXHR, textStatus, errorThrown) {
			alert(errorThrown);
		}
	});
}
/** How to send a PUT of XML asynchronously. */
function putXML(u, xml, done) {
	$.ajax({
		type : "PUT",
		url : u,
		async : false,
		cache : false,
		contentType : "application/xml",
		data : new XMLSerializer().serializeToString(xml),
		success : done,
		error : function(jqXHR, textStatus, errorThrown) {
			alert(errorThrown);
		}
	});
}
/** How to send a POST of XML asynchronously. */
function postXML(u, xml, done) {
	$.ajax({
		type : "POST",
		url : u,
		async : false,
		cache : false,
		contentType : "application/xml",
		data : new XMLSerializer().serializeToString(xml),
		success : done,
		error : function(jqXHR, textStatus, errorThrown) {
			alert(errorThrown);
		}
	});
}
/** How to send a DELETE asynchronously. */
function deleteUrl(u, done) {
	$.ajax({
		type : "DELETE",
		url : u,
		async : true,
		cache : false,
		success : done,
		error : function(jqXHR, textStatus, errorThrown) {
			alert(errorThrown);
		}
	});
}

/** Locates a URL with respect to the administrative REST interface. */
function where(tail) {
	return $("#admin")[0].href + "/" + tail;
}

// Make an XML element structure
// Derived from hack on Stack Overflow, but with extra tinkering
/** Function called to create a node in an XML structure. */
var Node;
/** Function called to create nodes in an XML structure from an array. */
var NodeAll;
(function() {
	var doc = document.implementation.createDocument(null, null, null);
	var adminNS = "http://ns.taverna.org.uk/2010/xml/server/admin/";
	Node = function() {
		var node = doc.createElementNS(adminNS, arguments[0]), child;
		for ( var i = 1; i < arguments.length; i++) {
			child = arguments[i];
			if (child == undefined)
				continue;
			if (typeof child != 'object')
				child = doc.createTextNode(child.toString());
			node.appendChild(child);
		}
		return node;
	};
	NodeAll = function(wrapperElem, wrappedElem, elements) {
		var node = doc.createElementNS(adminNS, wrapperElem);
		for ( var i = 0; i < elements.length; i++) {
			var child = doc.createElementNS(adminNS, wrappedElem);
			var text = doc.createTextNode(elements[i]);
			child.appendChild(text);
			node.appendChild(child);
		}
		return node;
	};
})();

var buttonlist = [ "allowNew", "logFaults", "logWorkflows" ];
var readonlies = [ "invokationCount", "lastExitCode", "runCount", "startupTime", "operatingCount" ];
var entries = [ "defaultLifetime", "executeWorkflowScript", "javaBinary",
		"registrationPollMillis", "registrationWaitSeconds", "registryHost",
		"registryPort", "runLimit", "runasPasswordFile", "serverForkerJar",
		"serverWorkerJar", "usageRecordDumpFile", "operatingLimit",
		"registryJar" ];
/** Cached information about users. */
var userinfo = [];
/** Extra arguments to pass to the runtime. */
var extraAry = [];

/** How to update the read-only fields; will be called periodically */
function updateRO() {
	$.each(readonlies, function(idx, val) {
		var widget = $("#" + val);
		getText(where(val), function(data) {
			widget.html(data);
		});
	});
	getJSON(where("factoryProcessMapping"),
			function(data) {
				var ary = data.stringList.string;
				var tbl = $("#factoryProcessMapping");
				tbl.html("<tr><th>User<th>ID</tr>");
				if (ary != undefined)
					for ( var i = 0; i < ary.length - 1; i += 2)
						tbl.append("<tr><td>" + ary[i] + "<td>" + ary[i + 1]
								+ "</tr>");
			});
}

/**
 * Generate a user row with suitable indices, but no content (it will be pushed
 * into the row later).
 */
function userRowHTML(idx) {
	// USER NAME
	var content = "<td><span id='username" + idx
			+ "' title='The login name of the user.'></span></td>";
	// SYSTEM ID MAPPING
	content += "<td><input id='userlocal"
			+ idx
			+ "' title='The system username to run workflows as, or blank for default.' /></td>";
	// ENABLED
	content += "<td><label title='Is this user allowed to log in?' for='useron"
			+ idx + "'>Enabled</label>" + "<input type='checkbox' id='useron"
			+ idx + "' /></td>";
	// ADMIN
	content += "<td><label title='Is this user an admin (allowed to access this page)?' for='useradmin"
			+ idx
			+ "'>Admin</label>"
			+ "<input type='checkbox' id='useradmin"
			+ idx + "' /></td>";
	// SET PASSWORD
	content += "<td><button title='Set the password for this user.' id='userpass"
			+ idx + "'>Set Password</button></td>";
	// DELETE
	content += "<td><button title='Delete this user. Take care to not delete yourself!' id='userdel"
			+ idx + "'>Delete</button></td>";

	return "<tr id='usersep" + idx + "' class='userrows'>"
			+ "<td colspan=6><hr></td></tr>" + "<tr id='userrow" + idx
			+ "' class='userrows'>" + content + "</tr>";
}

/** How to get the list of permitted workflows; called on demand */
function refreshWorkflows() {
	var wftable = $("#workflows"), wfbut = $("#saveWorkflows"), wfref = $("#refreshWorkflows");
	wfbut.button("disable");
	wfref.button("disable");
	getJSON(where("permittedWorkflowURIs"), function(data) {
		var s = "";
		$.each(data.stringList.string || [], function(idx, str) {
			s += $.trim(str) + "\n";
		});
		wftable.val($.trim(s));
		wfbut.button("enable");
		wfref.button("enable");
	});
}
/** How to set the list of permitted workflows; called when the user clicks */
function saveWorkflows() {
	var wftable = $("#workflows"), wfbut = $("#saveWorkflows");
	var xml = NodeAll("stringList", "string", wftable.val().split("\n"));
	wfbut.button("disable");
	putXML(where("permittedWorkflowURIs"), xml, function() {
		refreshWorkflows();
	});
}

/** How to update the table of users; called on demand */
function refreshUsers() {
	var usertable = $("#userList");
	getJSON(where("users"), function(data) {
		$(".userrows").remove();
		userinfo = [];
		$.each(data.userList.user, function(idx, url) {
			usertable.append(userRowHTML(idx));
			var i = idx;
			userinfo[i] = {
				url : url
			};
			getJSON(url, function(data) {
				var model = userinfo[i].model = data.userDesc;
				$("#username" + i).html(model.username);
				$("#userlocal" + i).val(model.localUserId).change(function() {
					updateUser(i, "localUserId", $(this).val());
				});
				$("#useron" + i).button().attr("checked", model.enabled)
						.button("refresh").click(
								function() {
									updateUser(i, "enabled", $(this).attr(
											"checked") == "checked");
								});
				$("#useradmin" + i).button().attr("checked", model.admin)
						.button("refresh").click(
								function() {
									updateUser(i, "admin", $(this).attr(
											"checked") == "checked");
								});
				$("#userpass" + i).button({
					icons : {
						primary : "ui-icon-alert"
					}
				}).click(function() {
					updatePasswordUser(i);
				});
				$("#userdel" + i).button({
					icons : {
						primary : "ui-icon-trash"
					},
					text : false
				}).click(function() {
					deleteUser(i);
				});
			});
			return true;
		});
	});
}

/** How to delete a user by index (with dialog) */
function deleteUser(idx) {
	$("#dialog-confirm").dialog({
		modal : true,
		autoOpen : false,
		buttons : {
			"OK" : function() {
				$(this).dialog("close");
				deleteUrl(userinfo[idx].url, function() {
					refreshUsers();
				});
			},
			"Cancel" : function() {
				$(this).dialog("close");
			}
		}
	});
	$("#dialog-confirm").dialog("open");
}

/** How to update a user's password by index (with dialog) */
function updatePasswordUser(idx) {
	$("#change-password").val("");
	$("#change-password2").val("");
	$("#dialog-password").dialog({
		modal : true,
		autoOpen : false,
		buttons : {
			"OK" : function() {
				$(this).dialog("close");
				var pass = $("#change-password").val();
				var pass2 = $("#change-password2").val();
				$("#change-password").val("");
				$("#change-password2").val("");
				if (pass.equals(pass2))
					updateUser(idx, "password", pass);
			},
			"Cancel" : function() {
				$(this).dialog("close");
				$("#change-password").val("");
			}
		}
	});
	$("#dialog-password").dialog("open");
}

/** How to set a specific field of a user record */
function updateUser(idx, field, value) {
	var model = userinfo[idx].model;
	var xml = Node("userDesc", Node("username", model.username),
			field == "password" ? Node("password", value) : undefined,
			field == "localUserId" ? Node("localUserId", value)
					: model.localUserId == undefined ? undefined : Node(
							"localUserId", model.localUserId), Node("enabled",
					field == "enabled" ? value : model.enabled), Node("admin",
					field == "admin" ? value : model.admin));
	putXML(userinfo[idx].url, xml, function() {
		refreshUsers();
	});
}

/** How to configure all the buttons and entries */
function connectButtonsAndEntries() {
	$.each(buttonlist, function(idx, val) {
		var widget = $("#" + val);
		var u = where(val);
		widget.button();
		getText(u, function(data) {
			widget.attr('checked', (data + "") != "false");
			widget.button("refresh");
		});
		widget.change(function() {
			putText(u, widget.attr("checked") == "checked", function(data) {
				widget.attr('checked', (data + "") != "false");
				widget.button("refresh");
				return true;
			});
		});
	});
	$.each(entries, function(idx, val) {
		var widget = $("#" + val);
		var u = where(val);
		getText(u, function(data) {
			widget.val(data);
		});
		widget.change(function() {
			putText(u, widget.val(), function(data) {
				widget.val(data);
				return true;
			});
		});
	});
}

/** What happens when the user tries to make a new user */
function makeNewUser() {
	var sysid = $("#newSysID").val();
	var newuserinfo = {
		admin : $("#newAdmin").attr("checked") == "checked",
		enabled : $("#newEnabled").attr("checked") == "checked",
		username : $("#newUsername").val(),
		password : $("#newPassword").val()
	};
	// Blank out the password immediately!
	$("#newPassword").val("");
	if (sysid.length > 0) {
		newuserinfo.localUserId = sysid;
	}
	if (newuserinfo.username == "" || newuserinfo.password == "") {
		alert("Won't create user; need a username and a password!");
		return;
	}
	var xml = newuserinfo = Node("userDesc", Node("username",
			newuserinfo.username), Node("password", newuserinfo.password),
			newuserinfo.localUserId == undefined ? undefined : Node(
					"localUserId", newuserinfo.localUserId), Node("enabled",
					newuserinfo.enabled), Node("admin", newuserinfo.admin));
	postXML(where("users"), xml, function() {
		refreshUsers();
	});
}

/** Handle the extra arguments */
function loadExtraArgs() {
	getJSON(where("extraArguments"),
			function(data) {
				var rows = data.stringList.string || [];
				if ((typeof rows) == "string")
					rows = [ rows ];
				$(".extraargrow").remove();
				extraAry = rows;
				var i;
				function row() {
					var buf = "<tr class='extraargrow'>";
					for ( var i = 1; i < arguments.length; i++)
						buf += "<td>" + arguments[i] + "</td>";
					return $(arguments[0]).append(buf + "</tr>");
				}
				function delbutn(id, what) {
					return "<button id='" + id + "' title='Delete this " + what
							+ ".'>Del</button>";
				}
				for (i = 0; i < extraAry.length; i++) {
					var rowid = "extradel" + i;
					if (rows[i].match("^-D")) {
						var m = rows[i].match("^-D([^=]*)=(.*)$");
						row("#extraArguments-prop", delbutn(rowid,
								"property assignment"), "<tt><b>-D</b>" + m[1]
								+ "<b>=</b>" + m[2] + "</tt>");
					} else if (rows[i].match("-E")) {
						var m = rows[i].match("^-E([^=]*)=(.*)$");
						row("#extraArguments-env", delbutn(rowid,
								"environment assignment"), "<tt><b>-E</b>"
								+ m[1] + "<b>=</b>" + m[2] + "</tt>");
					} else {
						var m = rows[i].match("^-J(.*)$");
						row("#extraArguments-runtime", delbutn(rowid,
								"runtime parameter"), "<tt><b>-J</b>" + m[1]
								+ "</tt>");
					}
					$("#" + rowid).button({
						icons : {
							primary : "ui-icon-trash"
						},
						text : false
					}).click(
							(function(row) {
								return function() {
									extraAry.splice(row, 1);
									var xml = NodeAll("stringList", "string",
											extraAry);
									putXML(where("extraArguments"), xml,
											function() {
												loadExtraArgs();
											});
								};
							})(i));
				}
			});
}

/** Run a dialog for creating an extra argument. */
function addExtraArg(dialogId, prefix, part1id, part2id) {
	$(dialogId).dialog({
		modal : true,
		autoOpen : false,
		buttons : {
			"OK" : function() {
				$(this).dialog("close");
				var str = prefix + $(part1id).val();
				if (part2id != undefined)
					str += "=" + $(part2id).val();
				extraAry.push(str);
				var xml = NodeAll("stringList", "string", extraAry);
				putXML(where("extraArguments"), xml, function() {
					loadExtraArgs();
				});
			},
			"Cancel" : function() {
				$(this).dialog("close");
			}
		}
	});
	$(dialogId).dialog("open");
}

/** Start everything going on page load */
$(function() {
	// Must be done in this order because the accordion is inside a tab
	$("#a-worker").accordion({
		collapsible : true,
		fillSpace : true,
		autoHeight : false
	});
	$("#body").tabs({
		selected : 0
	});
	$("#saveWorkflows").button({
		disabled : true
	}).click(function(event) {
		saveWorkflows();
		event.preventDefault();
	});
	$("#refreshWorkflows").button({
		disabled : true
	}).click(function(event) {
		refreshWorkflows();
		event.preventDefault();
	});

	// Make the link to the list of usage records point correctly
	// Original plan called for browsable table, but that's too slow
	$("#ur").attr("href", where("usageRecords"));

	connectButtonsAndEntries();
	updateRO();
	setInterval(updateRO, 30000);
	refreshUsers();
	refreshWorkflows();
	$("#newEnabled").button();
	$("#newAdmin").button({
		icons : {
			primary : "ui-icon-alert"
		}
	});
	$("#makeNewUser").button().click(function() {
		makeNewUser();
	});
	$("#extra-prop-add").button().click(function() {
		addExtraArg("#dialog-property", "-D", "#prop-key", "#prop-value");
	});
	$("#extra-env-add").button().click(function() {
		addExtraArg("#dialog-environment", "-E", "#env-key", "#env-value");
	});
	$("#extra-run-add").button().click(function() {
		addExtraArg("#dialog-runtime", "-J", "#runtime-value");
	});
	loadExtraArgs();
});
