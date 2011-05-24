package org.apache.maven.artifact.router.discovery;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.router.ArtifactRouter;
import org.apache.maven.artifact.router.ArtifactRouterException;
import org.apache.maven.artifact.router.GroupRoute;
import org.apache.maven.artifact.router.MirrorRoute;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

@Component( role = ArtifactRouterDiscoveryStrategy.class, hint = "dns" )
final class DNSDiscoveryStrategy
    implements ArtifactRouterDiscoveryStrategy
{
    
    @Requirement
    private Logger logger;

    public ArtifactRouter findRouter()
        throws ArtifactRouterException
    {
        final Map<String, String> env = new HashMap<String, String>();
        env.put( "java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory" );

        DirContext jndiContext;
        try
        {
            jndiContext = new InitialDirContext( new Hashtable<String, String>( env ) );
        }
        catch ( final NamingException e )
        {
            throw new ArtifactRouterException( "Failed to initialize JNDI context for mirror-router DNS lookups: "
                            + e.getMessage(), e );
        }

        InetAddress[] addresses;
        try
        {
            final InetAddress lh = InetAddress.getLocalHost();
            addresses = InetAddress.getAllByName( lh.getHostName() );
        }
        catch ( final UnknownHostException e )
        {
            throw new ArtifactRouterException( "Failed to retrieve local hostnames for mirror router: " + e.getMessage(),
                                             e );
        }

        for ( final InetAddress addr : addresses )
        {
            final String hostname = addr.getCanonicalHostName();

            final int idx = hostname.indexOf( '.' );
            if ( idx > -1 )
            {
                final String domain = hostname.substring( idx + 1 );
                final Attributes attrs;
                try
                {
                    attrs = jndiContext.getAttributes( "_maven." + domain, new String[] { "TXT" } );
                }
                catch ( final NamingException e )
                {
                    continue;
                }

                String txtRecord = null;
                try
                {
                    txtRecord = (String) attrs.get( "TXT" ).get();
                }
                catch ( final NamingException e )
                {
                }

                if ( txtRecord != null )
                {
                    return parse( txtRecord );
                }
            }
        }

        return null;
    }

    private ArtifactRouter parse( String record )
    {
        ArtifactRouter router = new ArtifactRouter();
        
        String[] sections = record.split(";");
        for ( String section : sections )
        {
            String[] kv = section.split( "=" );
            if ( kv.length < 2 )
            {
                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "Skipping DNS TXT section: '" + section + "': it is not a value key-value pair." );
                }
                
                continue;
            }
            
            if ( "mirrors".equalsIgnoreCase( kv[0] ) )
            {
                router.addMirrors( loadMirrorsFrom( kv[1] ) );
            }
            else if ( "groups".equalsIgnoreCase( kv[0] ) )
            {
                router.addGroups( loadGroupsFrom( kv[1] ) );
            }
        }
        
        return router;
    }

    private Collection<GroupRoute> loadGroupsFrom( String string )
    {
        // TODO Implement DNSDiscoveryStrategy.loadGroupsFrom
        throw new UnsupportedOperationException( "Not Implemented." );
    }

    private Collection<MirrorRoute> loadMirrorsFrom( String string )
    {
        // TODO Implement DNSDiscoveryStrategy.loadMirrorsFrom
        throw new UnsupportedOperationException( "Not Implemented." );
    }
    
}
