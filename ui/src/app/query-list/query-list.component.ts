import { Component, OnInit } from '@angular/core';
import {QueryService} from "../shared/query/query.service";

@Component({
  selector: 'app-query-list',
  templateUrl: './query-list.component.html',
  styleUrls: ['./query-list.component.scss']
})
export class QueryListComponent implements OnInit {
  queries: Array<any>;

  constructor(private queryService: QueryService) { }

  ngOnInit(): void {
    this.queryService.getAll().subscribe(data => {
      this.queries = data;
    })
  }

}
