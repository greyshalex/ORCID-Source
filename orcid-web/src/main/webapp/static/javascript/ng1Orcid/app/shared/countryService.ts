import { Injectable } 
    from '@angular/core';

import { Headers, Http, RequestOptions, Response } 
    from '@angular/http';

import { Observable } 
    from 'rxjs/Observable';

import 'rxjs/Rx';

@Injectable()
export class CountryService {
    private headers: Headers;
    private url: string;

    constructor( private http: Http ){
        this.headers = new Headers(
            { 
                'Content-Type': 'application/json' 
            }
        );
        this.url = getBaseUri() + '/account/countryForm.json';
    }

    getCountryData(): Observable<any> {
        return this.http.get(
            this.url
        )
        .map((res:Response) => res.json()).share();
    }

    setCountryData( obj ): Observable<any> {
        let encoded_data = JSON.stringify(obj);
        
        return this.http.post( 
            this.url, 
            encoded_data, 
            { headers: this.headers }
        )
        .map((res:Response) => res.json()).share();
    }
}
