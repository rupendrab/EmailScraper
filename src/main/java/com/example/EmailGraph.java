package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
// import java.util.Base64;
import java.util.Collections;
import java.util.List;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.Attachment;
import com.microsoft.graph.models.FileAttachment;
import com.microsoft.graph.models.MailFolder;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.requests.AttachmentCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;

public class EmailGraph
{
    // Configure authentication provider (you need to implement token retrieval using MSAL)
    String mailHost;
    String loginId;
    String clientId;
    String clientSecret;
    String tenantId;
    String folderName;
    
    GraphServiceClient<?> graphClient;
    
    public EmailGraph(String mailHost, String loginId, String clientId, String clientSecret, String tenantId,
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
        this.graphClient = getClient();
    }
    
    
    private GraphServiceClient<?> getClient()
    {
        // Build the ClientSecretCredential
        ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
            .clientId(clientId)
            .clientSecret(clientSecret)
            .tenantId(tenantId)
            .build();

        // Create a TokenCredentialAuthProvider using the ClientSecretCredential
        TokenCredentialAuthProvider authProvider = new TokenCredentialAuthProvider(
            Collections.singletonList("https://graph.microsoft.com/.default"),
            clientSecretCredential
        );
        
        // Initialize the Graph client with the authentication provider
        GraphServiceClient<?> graphClient = GraphServiceClient
            .builder()
            .authenticationProvider(authProvider)
            .buildClient();
        
        return graphClient;
    }
    
    public List<Message> getMessages()
    {
        List<String> folderNames = Arrays.asList(folderName.split("/"));
        int pos = 0;
        String folderNameCurrent = folderNames.get(pos);
        String folderId = null;
        List<Message> messages = null;
        while (true)
        {
            if (pos < folderNames.size() - 1)
            {
                List<MailFolder> folders = graphClient
                        .users(loginId)
                        .mailFolders(folderNameCurrent)
                        .childFolders()
                        .buildRequest()
                        .get()
                        .getCurrentPage();
                String nextFolderName = folderNames.get(pos + 1);
                // System.out.println("Checking for folder name: " + nextFolderName);
                for (MailFolder folder : folders) 
                {
                    // System.out.println("Folder Name: " + folder.displayName + ", Folder ID: " + folder.id);  
                    if (nextFolderName.equals(folder.displayName))
                    {
                        pos += 1;
                        folderNameCurrent = nextFolderName;
                        folderId = folder.id;
                        // System.out.println("Set Folder Name: " + folderNameCurrent + ", Folder ID: " + folderId);
                        break;
                    }
                }
            }
            else
            {
                // System.out.println("Getting messages for Folder Name: " + folderNameCurrent + ", Folder ID: " + folderId);
                messages = graphClient
                        .users(loginId)
                        .mailFolders(folderId)
                        .messages()
                        .buildRequest()
                        .get()
                        .getCurrentPage();
                break;
            }
        }
        if (messages == null)
        {
            return new ArrayList<>();
        }
        return messages;
    }
    
    public void processMessages()
    {
        List<Message> messages = getMessages();
        for (Message message : messages) 
        {
            System.out.println("Subject: " + message.subject);

            // Fetch attachments for the message
            AttachmentCollectionPage attachments = graphClient
                .users(loginId)
                .messages(message.id)  // Access the specific message by ID
                .attachments()
                .buildRequest()
                .get();

            // Iterate through the attachments and print the names
            for (Attachment attachment : attachments.getCurrentPage()) 
            {
                if (! (attachment instanceof FileAttachment)) continue;
                
                FileAttachment fileAttachment = (FileAttachment) attachment;

                // Print attachment name
                if (! fileAttachment.name.matches("^.*xlsx?$")) continue;
                System.out.println("Downloading attachment: " + fileAttachment.name);

                // Decode the base64-encoded content
                byte[] fileData = fileAttachment.contentBytes;

                // Save the file to the local filesystem
                saveToFile(fileAttachment.name, fileData);
            }
        }
    }
    
    private static void saveToFile(String fileName, byte[] fileData) 
    {
        try 
        {
            Path filePath = Paths.get("c:/tmp/KAM/attachments", fileName);  
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, fileData);
            System.out.println("File saved: " + filePath.toString());
        } 
        catch (IOException e) 
        {
            System.out.println("Failed to save file: " + e.getMessage());
        }
    }
    
    private static void printHelp()
    {
        System.err.println("Usage: java " + EmailGraph.class.getName() + " \n" +
                "    -H (show help) \n" +
                "    -h <Mail Host> default is outlook.office365.com \n" +
                "    -u <Login ID> \n" +
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
            System.err.println("Login ID Id is not provided");
            printHelp();
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
        EmailGraph emailGraph = new EmailGraph(mailHost, loginId, clientId, clientSecret, tenantId, folderName);
        emailGraph.processMessages();
        System.out.println("Done!");
    }
    
}
