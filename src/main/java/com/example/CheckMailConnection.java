package com.example;

import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IClientCredential;


public class CheckMailConnection
{
    String mailHost;
    String loginId;
    String clientId;
    String clientSecret;
    String tenantId;
    String folderName;
    
    Session mailSesion;
    Store mailStore;
    Folder folder;
    
    public CheckMailConnection(String mailHost, String loginId, String clientId, String clientSecret, String tenantId,
            String folderName)
    {
        super();
        this.mailHost = mailHost;
        if (this.mailHost == null || this.mailHost.isEmpty())
        {
            this.mailHost = "outlook.office365.com";
        }
        this.loginId = loginId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tenantId = tenantId;
        this.folderName = folderName;
    }
    
    private String retrieveToken() throws MalformedURLException, InterruptedException, ExecutionException
    {
        String authority = String.format("https://login.microsoftonline.com/%s", tenantId);
        IClientCredential clientCredential = ClientCredentialFactory.createFromSecret(clientSecret);
        ConfidentialClientApplication app = ConfidentialClientApplication.builder(clientId, clientCredential)
            .authority(authority)
            .build();
        Set<String> scopes = new HashSet<>();
        scopes.add("https://outlook.office365.com/.default");
        ClientCredentialParameters params = ClientCredentialParameters.builder(scopes).build();
        return app.acquireToken(params).get().accessToken();
    }

    private void connectToMailstore(Store mailStore, int maxAttempts, int intervalSeconds) throws MessagingException, InterruptedException, MalformedURLException, ExecutionException
    {
        String token = null;
        token = retrieveToken();
        System.out.println("Success in retrieving token for exchange login");
        int noAttempts = 0;
        String emailPassword = token;
        while (true)
        {
            try
            {
                noAttempts += 1;
                mailStore.connect(mailHost, loginId, emailPassword);
                return;
            }
            catch (MessagingException e)
            {
                boolean retry = noAttempts < maxAttempts;
                System.out.println(String.format("Unable to connect to mail store (attempt %d of %d): %s", 
                        noAttempts, maxAttempts, e.getMessage()));
                if (! retry)
                {
                    throw e;
                }
                Thread.sleep(intervalSeconds * 1000); 
            }
        }
    }
    
    private void connect() throws Exception
    {
        Properties props = new Properties();        
        props.put("mail.debug.auth", "true");
        props.put("mail.imap.ssl.enable", "true");
        props.put("mail.imap.ssl.mechanisms", "XOAUTH2");
        props.put("mail.imap.auth.plain.disable", "true");
        props.put("mail.imap.auth.xoauth2.disable", "false");
        props.put("mail.imap.ssl.enable", "true");

        Session mailSession = Session.getDefaultInstance(props, null);
        mailSession.setDebug(true);
        mailStore = mailSession.getStore("imap");
        
        connectToMailstore(mailStore, 1, 1);
        
        System.out.println("Is connected = " + mailStore.isConnected());
        this.folder = navigateTo();
        folder.open(Folder.READ_ONLY);
        int totalMessages = folder.getMessageCount();
        System.out.println(String.format("Number of messages in %s = %d", folderName, totalMessages));
    }
    
    private Folder navigateTo() throws MessagingException
    {
        String[] path = folderName.split("\\\\");
        Folder folder = mailStore.getDefaultFolder();
        for (String pathPart : path)
        {
            folder = folder.getFolder(pathPart);
        }
        return folder;
    }
    
    private static void printHelp()
    {
        System.err.println("Usage: java " + CheckMailConnection.class.getName() + " \n" +
                "    -H (show help) \n" +
                "    -h <Mail Host> default is outlook.office365.com \n" +
                "    -u <Login ID> default is KAMResearch2@kochind.onmicrosoft.com \n" +
                "    -cli <Client Id> \n" +
                "    -cls <Client Secret> \n" +
                "    -ti <Tenant Id> \n " +
                "    -f <Folder Name> default is INBOX/ABI"
                );
        System.exit(1);
    }
    
    public static void main(String[] args) throws Exception
    {
        ArgParse argParse = new ArgParse(args);
        if (argParse.existsKey("H")) printHelp();
        String mailHost = argParse.get("h");
        if (mailHost == null) mailHost = "outlook.office365.com";
        String loginId = argParse.get("u");
        if (loginId == null || loginId.isEmpty())
        {
            loginId = "KAMResearch2@kochind.onmicrosoft.com";
        }
        String clientId = argParse.get("cli");
        String clientSecret = argParse.get("cls");
        String tenantId = argParse.get("ti");
        if (clientId == null || clientId.isEmpty())
        {
            System.err.println("Client Id is not provided");
            printHelp();
        }
        if (clientSecret == null || clientSecret.isEmpty())
        {
            System.err.println("Client Secret is not provided");
            printHelp();
        }
        if (tenantId == null || tenantId.isEmpty())
        {
            System.err.println("Tenant ID is not provided");
            printHelp();
        }
        String folderName = argParse.get("f");
        if (folderName == null || folderName.isEmpty())
        {
            folderName = "INBOX/ABI";
        }
        CheckMailConnection checkMailConnection = new CheckMailConnection(mailHost, loginId, clientId, clientSecret, tenantId, folderName);
        checkMailConnection.connect();
    }
}
