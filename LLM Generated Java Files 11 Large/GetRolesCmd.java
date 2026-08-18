package org.keycloak.client.admin.cli.commands;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.keycloak.client.admin.cli.config.ConfigData;
import org.keycloak.client.admin.cli.operations.ClientOperations;
import org.keycloak.client.admin.cli.operations.GroupOperations;
import org.keycloak.client.admin.cli.operations.RoleOperations;
import org.keycloak.client.admin.cli.operations.UserOperations;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import static org.keycloak.client.admin.cli.util.AuthUtil.ensureToken;
import static org.keycloak.client.admin.cli.util.ConfigUtil.DEFAULT_CONFIG_FILE_STRING;
import static org.keycloak.client.admin.cli.util.ConfigUtil.credentialsAvailable;
import static org.keycloak.client.admin.cli.util.ConfigUtil.loadConfig;
import static org.keycloak.client.admin.cli.util.HttpUtil.composeResourceUrl;
import static org.keycloak.client.admin.cli.util.OsUtil.CMD;
import static org.keycloak.client.admin.cli.util.OsUtil.PROMPT;

/**
 * Implements the 'get-roles' kcadm subcommand: resolves the correct Admin REST
 * role-mappings URL for a user/group/composite-role/client selector combination
 * and delegates request execution/output formatting to {@link GetCmd}.
 */
@CommandDefinition(name = "get-roles", description = "[ARGUMENTS]")
public class GetRolesCmd extends GetCmd {

    @Option(name = "uusername", hasValue = true)
    String uusername;
    @Option(name = "uid", hasValue = true)
    String uid;

    @Option(name = "cclientid", hasValue = true)
    String cclientid;
    @Option(name = "cid", hasValue = true)
    String cid;

    @Option(name = "rname", hasValue = true)
    String rname;
    @Option(name = "rid", hasValue = true)
    String rid;

    @Option(name = "gname", hasValue = true)
    String gname;
    @Option(name = "gpath", hasValue = true)
    String gpath;
    @Option(name = "gid", hasValue = true)
    String gid;

    @Option(name = "rolename", hasValue = true)
    String rolename;
    @Option(name = "roleid", hasValue = true)
    String roleid;

    @Option(name = "available", hasValue = false)
    boolean available;
    @Option(name = "effective", hasValue = false)
    boolean effective;
    @Option(name = "all", hasValue = false)
    boolean all;

    @Override
    void initOptions() {
        super.initOptions();
        // This command computes its URL programmatically instead of taking a positional
        // argument, so a placeholder is prepended to satisfy GetCmd's generic validation.
        if (args == null) {
            args = new ArrayList();
        }
        args.add(0, "uri");
    }

    @Override
    void processOptions(CommandInvocation commandInvocation) {
        if (uid != null && uusername != null) {
            throw new IllegalArgumentException("Options --uid and --uusername are mutually exclusive");
        }
        if (gid != null && gname != null) {
            throw new IllegalArgumentException("Options --gid and --gname are mutually exclusive");
        }
        if (gid != null && gpath != null) {
            throw new IllegalArgumentException("Options --gid and --gpath are mutually exclusive");
        }
        if (gname != null && gpath != null) {
            throw new IllegalArgumentException("Options --gname and --gpath are mutually exclusive");
        }
        if (roleid != null && rolename != null) {
            throw new IllegalArgumentException("Options --roleid and --rolename are mutually exclusive");
        }
        if (rid != null && rname != null) {
            throw new IllegalArgumentException("Options --rid and --rname are mutually exclusive");
        }
        if (cid != null && cclientid != null) {
            throw new IllegalArgumentException("Options --cid and --cclientid are mutually exclusive");
        }
        if (isUserSpecified() && isGroupSpecified()) {
            throw new IllegalArgumentException("Options for user and group are mutually exclusive");
        }
        if (isUserSpecified() && isCompositeRoleSpecified()) {
            throw new IllegalArgumentException("Options for user and composite role are mutually exclusive");
        }
        if (isGroupSpecified() && isCompositeRoleSpecified()) {
            throw new IllegalArgumentException("Options for group and composite role are mutually exclusive");
        }
        if (all && effective) {
            throw new IllegalArgumentException("Options --all and --effective are mutually exclusive");
        }
        if (all && available) {
            throw new IllegalArgumentException("Options --all and --available are mutually exclusive");
        }
        super.processOptions(commandInvocation);
    }

    @Override
    public CommandResult process(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        ConfigData config = loadConfig();
        config = copyWithServerInfo(config);
        setupTruststore(config, commandInvocation);

        ConfigData currentConfig = ensureAuthInfo(config, commandInvocation);
        if (credentialsAvailable(currentConfig)) {
            currentConfig = ensureToken(currentConfig);
        }

        String realm = getTargetRealm(currentConfig);
        String root = composeAdminRoot(currentConfig);

        String url;
        if (isUserSpecified()) {
            if (uid == null) {
                uid = UserOperations.getIdFromUsername(currentConfig, root, realm, uusername);
            }
            if (isClientSpecified()) {
                if (cid == null) {
                    cid = ClientOperations.getIdFromClientId(currentConfig, root, realm, cclientid);
                }
                url = composeResourceUrl(root, realm, "users/" + uid + "/role-mappings/clients/" + cid
                        + (available ? "/available" : effective ? "/composite" : ""));
            } else {
                url = composeResourceUrl(root, realm, "users/" + uid + "/role-mappings/realm"
                        + (available ? "/available" : effective ? "/composite" : ""));
            }
        } else if (isGroupSpecified()) {
            if (gid == null) {
                if (gname != null) {
                    gid = GroupOperations.getIdFromName(currentConfig, root, realm, gname);
                } else if (gpath != null) {
                    gid = GroupOperations.getIdFromPath(currentConfig, root, realm, gpath);
                }
            }
            if (isClientSpecified()) {
                if (cid == null) {
                    cid = ClientOperations.getIdFromClientId(currentConfig, root, realm, cclientid);
                }
                url = composeResourceUrl(root, realm, "groups/" + gid + "/role-mappings/clients/" + cid
                        + (available ? "/available" : effective ? "/composite" : ""));
            } else {
                url = composeResourceUrl(root, realm, "groups/" + gid + "/role-mappings/realm"
                        + (available ? "/available" : effective ? "/composite" : ""));
            }
        } else if (isCompositeRoleSpecified()) {
            if (available || effective) {
                throw new IllegalArgumentException("Options --available / --effective are not supported for composite roles");
            }
            if (isClientSpecified()) {
                if (cid == null) {
                    cid = ClientOperations.getIdFromClientId(currentConfig, root, realm, cclientid);
                }
                if (rid == null && isRoleSpecified()) {
                    rid = rolename != null ? rolename : RoleOperations.getClientRoleNameFromId(currentConfig, root, realm, cid, roleid);
                }
                url = composeResourceUrl(root, realm, "clients/" + cid + "/roles/" + rname + "/composites");
            } else {
                url = composeResourceUrl(root, realm, "roles/" + rname + "/composites");
            }
        } else {
            url = composeResourceUrl(root, realm, "roles");
        }

        super.url = url;
        return super.process(commandInvocation);
    }

    private boolean isRoleSpecified() {
        return rolename != null || roleid != null;
    }

    private boolean isClientSpecified() {
        return cclientid != null || cid != null;
    }

    private boolean isGroupSpecified() {
        return gname != null || gpath != null || gid != null;
    }

    private boolean isCompositeRoleSpecified() {
        return rname != null || rid != null;
    }

    private boolean isUserSpecified() {
        return uusername != null || uid != null;
    }

    @Override
    protected String suggestHelp() {
        return "";
    }

    @Override
    protected boolean nothingToDo() {
        return false;
    }

    @Override
    protected String help() {
        return usage();
    }

    public static String usage() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " get-roles [ARGUMENTS]");
        out.println();
        out.println("Command to list realm or client roles available to a user, group, or composite role.");
        out.println();
        out.println("Arguments:");
        out.println("  --uusername USERNAME       Username of user");
        out.println("  --uid ID                    ID of user");
        out.println("  --cclientid CLIENTID        Client id of a client (not database ID)");
        out.println("  --cid ID                    ID of client (database ID)");
        out.println("  --rname NAME                Name of composite role");
        out.println("  --rid ID                    ID of composite role");
        out.println("  --gname NAME                Name of top level group");
        out.println("  --gpath PATH                Path of group");
        out.println("  --gid ID                    ID of group");
        out.println("  --rolename NAME             Name of specific role to check");
        out.println("  --roleid ID                 ID of specific role to check");
        out.println("  --available                 List available roles");
        out.println("  --effective                 List effective roles");
        out.println("  --all                       List all roles");
        out.println();
        out.println("Examples:");
        out.println();
        out.println(PROMPT + " " + CMD + " get-roles --uusername testuser");
        out.println(PROMPT + " " + CMD + " get-roles --uusername testuser --cclientid account --available");
        out.println();
        out.println("Use '" + CMD + " config credentials' to establish an authenticated session, or use --no-config, --server, " +
                "--realm, --user, and --password / --client with each invocation. Config file location can be overridden with " +
                DEFAULT_CONFIG_FILE_STRING + ".");
        out.flush();
        return sb.toString();
    }
}
