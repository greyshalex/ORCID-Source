orcidNgModule.filter('clean', function($filter){
   return function(x, idx){
       console.log(idx);
       
       return x;
   }; 
});