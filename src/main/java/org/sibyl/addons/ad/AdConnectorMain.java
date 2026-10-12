package org.sibyl.addons.ad;

import java.util.List;

/**
 * Standalone operator-triggered LDAPS snapshot CLI. Environment-only secrets.
 * --health contacts AD and returns bounded aggregate counts (not personal data).
 * --snapshot streams selected directory objects as JSON when explicitly requested.
 */
public final class AdConnectorMain {
    private AdConnectorMain() {}
    private static String json(String x) {
        StringBuilder out=new StringBuilder("\"");
        for(char ch: (x==null?"":x).toCharArray()){
            if(ch=='"'||ch=='\\'){out.append('\\').append(ch);}
            else if(ch=='\n')out.append("\\n");
            else if(ch=='\r')out.append("\\r");
            else if(ch=='\t')out.append("\\t");
            else if(ch<32)out.append(String.format("\\u%04x",(int)ch));
            else out.append(ch);
        }
        return out.append('"').toString();
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=1 || (!"--health".equals(args[0]) && !"--snapshot".equals(args[0])))
            throw new IllegalArgumentException("Use --health or --snapshot; credentials via SIBYL_AD_* environment only");
        AdDirectoryProvider provider=new AdDirectoryProvider(AdSettings.fromEnvironment());
        AdDirectoryProvider.Snapshot s=provider.readSnapshot();
        if("--health".equals(args[0])){
            System.out.println("LDAPS_OK users="+s.users().size()+
                " groups="+s.groups().size()+" computers="+s.computers().size());
            return;
        }
        StringBuilder json=new StringBuilder("{\"schema\":1,\"users\":[");
        boolean first=true;
        for(var u:s.users()){
            if(!first)json.append(',');
            first=false;
            json.append("{\"externalId\":").append(json(u.externalId()))
                .append(",\"accountName\":").append(json(u.accountName()))
                .append(",\"displayName\":").append(json(u.displayName()))
                .append(",\"enabled\":").append(u.enabled())
                .append('}');
        }
        json.append("],\"groups\":[");first=true;
        for(var g:s.groups()){
            if(!first)json.append(',');
            first=false;
            json.append("{\"externalId\":").append(json(g.externalId()))
                .append(",\"name\":").append(json(g.name()))
                .append(",\"members\":[");
            boolean memberFirst=true;
            for(String member:g.memberDistinguishedNames()){
                if(!memberFirst)json.append(',');
                memberFirst=false;json.append(json(member));
            }
            json.append("]}");
        }
        json.append("],\"computers\":[");first=true;
        for(var c:s.computers()){
            if(!first)json.append(',');
            first=false;
            json.append("{\"externalId\":").append(json(c.externalId()))
                .append(",\"name\":").append(json(c.name()))
                .append(",\"dnsHostName\":").append(json(c.dnsHostName()))
                .append(",\"operatingSystem\":").append(json(c.operatingSystem())).append('}');
        }
        System.out.println(json.append("]}"));
    }
}
