var webpack = require('webpack');

module.exports = {
    entry: "./require.js",
    module: {
        loaders: [
            { 
                test: /\.ts$/, 
                loader: 'ts-loader' 
            }
        ]
    },
    output: {
        path: "../.",
        filename: "angular_orcid_generated.js"
    },
    plugins: [
        function()
        {
            this.plugin("done", function(stats)
            {
                if (stats.compilation.errors && stats.compilation.errors.length)
                {
                    console.log(stats.compilation.errors[0].message);
                    process.exit(1);
                }
            });
        },
        new webpack.DefinePlugin({
            'NODE_ENV': JSON.stringify(process.env.NODE_ENV),
            'process.env':{
                'NODE_ENV': JSON.stringify(process.env.NODE_ENV)
            }
        })        
    ],
    resolve: {
        alias: {
            "@angular/upgrade/static": "@angular/upgrade/bundles/upgrade-static.umd.js"
        }
    },
    watch: false
}
