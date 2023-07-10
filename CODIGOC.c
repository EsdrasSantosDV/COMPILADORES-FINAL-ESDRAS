#include <stdio.h>

int main(){

	int TempoEmAnos, ValorSalario, i;
	scanf("%d %d ", &TempoEmAnos, &ValorSalario);
	printf("%d ", TempoEmAnos);

	if((TempoEmAnos*10)+5<=(10*10)+1) {

	if(TempoEmAnos>1) {

	ValorSalario=100;
	}
	}
	else{

	ValorSalario=ValorSalario*2;

	}

	for(ValorSalario=5;ValorSalario<=50;ValorSalario++){
	printf("%d ", ValorSalario);
	}

	while(i<10){

	printf("%d ", i);

	i=i+1;
	}


	do {
	
	printf("%d ", i);

	i=i+1;

	}while(i>=10);

}

