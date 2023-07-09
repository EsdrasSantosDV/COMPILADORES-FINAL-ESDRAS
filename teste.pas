Program CalcularSalario
Var TempoEmAnos, ValorSalario : Integer;
Begin
  If (TempoEmAnos > 10) Then
  Begin
    ValorSalario := 100;
  End
  Else
  Begin
    ValorSalario := ValorSalario * 2;
  End;
  Write(ValorSalario);
End.