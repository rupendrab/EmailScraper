package com.example;

import java.util.ArrayList;
import java.util.HashMap;

public class ArgParse
{
    String[] args;
    HashMap<String, String> parsed = new HashMap<>();
    ArrayList<String> positionalArguments;
    
    public ArgParse()
    {
        super();
    }

    public ArgParse(String[] args)
    {
        super();
        this.args = args;
        if (args != null)
        {
            parse();
        }
    }

    private void parse()
    {
        parsed = new HashMap<>();
        positionalArguments = new ArrayList<>();
        boolean isKey = false;
        String key = null;
        for (int i=0; i<args.length; i++)
        {
            if (args[i].startsWith("-"))
            {
                if (isKey)
                {
                   parsed.put(key, null); 
                }
                key = args[i].replaceAll("^\\-*", "");
                isKey = true;
            }
            else if (isKey)
            {
                parsed.put(key, args[i]); 
                isKey = false;
            }
            else
            {
                positionalArguments.add(args[i]);
            }
        }
        if (isKey)
        {
            parsed.put(key, null); 
        }
    }
    
    public boolean existsKey(String key)
    {
        return parsed.containsKey(key);
    }
    
    public String get(String key)
    {
        return parsed.get(key);
    }
    
    public String get(int position)
    {
        if (position < positionalArguments.size())
        {
            return positionalArguments.get(position);
        }
        return null;
    }

}
