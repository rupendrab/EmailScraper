## This codebase is to test access to an outlook 365 e-mail inbox through a registerd app.

The following need to be set up for the app based access: 
1. Client ID
2. Client Secret
3. Tenant ID
4. Email-ID
5. Login ID (If different from the e-mail ID)

The jar files created should work for Java 1.8+ versions.  
  
Example usage:

```
java -cp target\email-scraper-1.0-SNAPSHOT.jar com.example.CheckMailConnection -H
```

Shows the output:
```
Usage: java com.example.CheckMailConnection
    -H (show help)
    -h <Mail Host> default is outlook.office365.com
    -u <Login ID> default is KAMResearch2@kochind.onmicrosoft.com
    -cli <Client Id>
    -cls <Client Secret>
    -ti <Tenant Id>
     -f <Folder Name> default is INBOX/ABI
```


Also, the file test_generic.bat could be used directly on a windows system.  

