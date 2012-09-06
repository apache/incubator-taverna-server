// Wrappers round AJAX calls to simplify *most* of this stuff

// How to retrieve text
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
// How to retrieve JSON
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
// How to send a PUT of text
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
// How to send a PUT of XML
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
// How to send a POST of XML
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
// How to send a DELETE
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

// Make an XML element structure
// Derived from hack on Stack Overflow, but with extra tinkering
var Node;
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
	}
})();

// Locates a URL with respect to the administrative REST interface
function where(tail) {
	return $("#admin")[0].href + "/" + tail;
}

var userinfo = {};
var buttonlist = [ "allowNew", "logFaults", "logWorkflows" ];
var readonlies = [ "invokationCount", "lastExitCode", "runCount", "startupTime" ];
var entries = [ "defaultLifetime", "executeWorkflowScript", "javaBinary",
		"registrationPollMillis", "registrationWaitSeconds", "registryHost",
		"registryPort", "runLimit", "runasPasswordFile", "serverForkerJar",
		"serverWorkerJar", "usageRecordDumpFile" ];

// How to update the read-only fields; will be called periodically
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

// How to update the table of users; called on demand
function refreshUsers() {
	var usertable = $("#userList");
	getJSON(where("users"), function(data) {
		$(".userrows").remove();
		userinfo = {};
		$.each(data.userList.user, function(idx, url) {
			usertable.append("<tr id='usersep" + idx + "' class='userrows'>"
					+ "<td colspan=6><hr></td></tr>" + "<tr id='userrow" + idx
					+ "' class='userrows'>" + "<td><span id='username" + idx
					+ "'></span></td>" + "<td><input id='userlocal" + idx
					+ "' /></td>" + "<td><label for='useron" + idx
					+ "'>Enabled</label>" + "<input type='checkbox' id='useron"
					+ idx + "' /></td>" + "<td><label for='useradmin" + idx
					+ "'>Admin</label>"
					+ "<input type='checkbox' id='useradmin" + idx
					+ "' /></td>" + "<td><button id='userpass" + idx
					+ "'>Set Password</button></td>"
					+ "<td><button id='userdel" + idx
					+ "'>Delete</button></td>" + "</tr>");
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

// How to delete a user by index (with dialog)
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

// How to update a user's password by index (with dialog)
function updatePasswordUser(idx) {
	$("#change-password").val("");
	$("#dialog-password").dialog({
		modal : true,
		autoOpen : false,
		buttons : {
			"OK" : function() {
				$(this).dialog("close");
				var pass = $("#change-password").val();
				$("#change-password").val("");
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

// How to set a specific field of a user record
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

// How to configure all the buttons and entries
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
		})
	});
}

// What happens when the user tries to make a new user
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

	// Make the link to the list of usage records point correctly
	// Original plan called for browsable table, but that's too slow
	$("#ur").attr("href", where("usageRecords"));

	connectButtonsAndEntries();
	updateRO();
	setInterval(updateRO, 30000);
	refreshUsers();
	$("#newEnabled").button();
	$("#newAdmin").button({
		icons : {
			primary : "ui-icon-alert"
		}
	});
	$("#makeNewUser").button().click(function() {
		makeNewUser()
	});
	getJSON(where("extraArguments"), function(data) {
		var ary = data.stringList.string;
		var s = "";
		if (ary != undefined)
			for ( var i = 0; i < ary.length; i++)
				s += ary[i] + "\n";
		$("#extraArguments").text(s);
	});
});
